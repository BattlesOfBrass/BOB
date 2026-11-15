package de.idiotischer.bob.player.tasks.impl;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.player.tasks.Task;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.troop.ServerTroopManager;
import de.idiotischer.bob.troop.TroopStack;

import java.awt.Point;
import java.util.*;

//TODO: later use frontlien system so its way simple (ai just makes an attack order on its existing frontlien (or it creates one)
public class AttackTask implements Task {

    private static final long TPD = 1440L;

    private static final long PLAN = TPD / 8;
    private static final long LAUNCH_CD = TPD / 2;
    private static final long GATHER_TIMEOUT = 2 * TPD;

    private static final double RESERVE_FRACTION = 0.25;
    private static final int MAX_STACKS_PER_OFFENSIVE = 6;
    private static final int MIN_STACKS_VS_DEFENDED_TILE = 2;

    private static final double MIN_READY = 0.7;
    private static final double ATTACK_RATIO = 1.3;
    private static final double LAUNCH_RATIO = 1.0;
    private static final double DEF_WEIGHT = 0.5;

    private static final double MAX_HP_PER_TROOP = 200;
    private static final double MAX_ORG_PER_TROOP = 50;

    //uhm, so basically these are fgor targeting, these wont exist as soon as frontlines are added, its just so ai has a clear target
    private static final double VP_SCORE = 10;
    private static final double CITY_SCORE = 15;
    private static final double CLAIM_CORE_SCORE = 20;
    private static final double SCORE_DEF = 0.02;
    private static final double DISTANCE_SCORE = 0.005;

    private static final double PRIO = 95;

    private final Map<String, Operation> operations = new HashMap<>();
    private final Map<String, Operation> pending = new HashMap<>();
    private final Map<String, Long> nextTick = new HashMap<>();
    private final Map<Tile, Point> centres = new HashMap<>();

    @Override
    public boolean canExecute(String country) {
        Server server = Server.getInstance();

        Country me = server.getCountryManager().byAbbreviation(country);
        if (me == null || me.isCapitulated() || !server.getWarManager().isAtWar(country) || server.getConferenceManager().anyActive()) {
            operations.remove(country);
            pending.remove(country);
            return false;
        }

        long now = server.getGameManager().getTimeTicks();

        Operation op = operations.get(country);
        if (op != null) return isOperationDue(op, now);

        if (pending.containsKey(country)) return true;

        if (now < nextTick.getOrDefault(country, 0L)) return false;
        nextTick.put(country, now + PLAN);

        Operation plan = plan(me, now);
        if (plan == null) return false;

        pending.put(country, plan);
        return true;
    }

    @Override
    public double getPriority(String country) {
        return PRIO;
    }

    @Override
    public void tick(String country) {
        Server server = Server.getInstance();
        long now = server.getGameManager().getTimeTicks();

        Operation op = operations.get(country);

        if (op == null) {
            Operation plan = pending.remove(country);
            if (plan == null) return;

            operations.put(country, plan);
            orderGather(plan);
            return;
        }

        launch(country, op, now);
    }

    @Override
    public boolean finished(String country) {
        return true;
    }

    private boolean isOperationDue(Operation op, long now) {
        op.stacks.removeIf(s -> !s.isAlive() || !Server.getInstance().getTroopManager().has(s));

        if (op.stacks.isEmpty()) return true;
        if (now - op.startedAt >= GATHER_TIMEOUT) return true;

        ServerTroopManager tm = Server.getInstance().getTroopManager();

        for (TroopStack s : op.stacks) if (!s.getTile().equals(op.staging) || tm.isMoving(s)) return false;

        return true;
    }

    private void orderGather(Operation op) {
        ServerTroopManager tm = Server.getInstance().getTroopManager();

        Iterator<TroopStack> it = op.stacks.iterator();

        while (it.hasNext()) {
            TroopStack s = it.next();

            if (s.getTile().equals(op.staging)) continue;

            List<Tile> path = tm.findPath(s, op.staging, Server.getInstance().getTileManager());

            if (path == null || path.size() < 2) {
                it.remove();
                continue;
            }

            tm.addTroopPath(s, path);
            tm.startMovement(s);
        }
    }

    private void launch(String country, Operation op, long now) {
        operations.remove(country);

        Server server = Server.getInstance();
        ServerTroopManager tm = server.getTroopManager();

        op.stacks.removeIf(s -> !s.isAlive() || !tm.has(s));

        List<TroopStack> ready = new ArrayList<>();

        for (TroopStack s : op.stacks) if (s.getTile().equals(op.staging) && !tm.isMoving(s) && !server.getCombatManager().isInCombat(s) && !s.isBroken()) ready.add(s);

        Country me = server.getCountryManager().byAbbreviation(country);
        Country targetOwner = op.target.getController();

        if (me == null || targetOwner == null || !server.getWarManager().isAtWar(me, targetOwner)) return;

        double defenders = defenderPower(me, op.target);

        boolean defended = defenders > 0;
        int needed = defended ? MIN_STACKS_VS_DEFENDED_TILE : 1;

        double ours = ready.stream().mapToDouble(AttackTask::power).sum();

        if (ready.size() < needed || ours < defenders * LAUNCH_RATIO) {
            nextTick.put(country, now + PLAN);
            return;
        }

        for (TroopStack s : ready) {
            List<Tile> path = tm.findPath(s, op.target, server.getTileManager());

            if (path == null || path.size() < 2) continue;

            tm.addTroopPath(s, path);
            tm.startMovement(s);
        }

        nextTick.put(country, now + LAUNCH_CD);
    }


    private Operation plan(Country me, long now) {
        Server server = Server.getInstance();
        String abbr = me.getAbbreviation();

        List<TroopStack> all = server.getTroopManager().getForController(abbr);
        if (all.isEmpty()) return null;

        List<TroopStack> idle = new ArrayList<>();

        for (TroopStack s : all) {
            if (!s.isAlive() || s.isBroken()) continue;
            if (server.getTroopManager().isMoving(s) || server.getCombatManager().isInCombat(s)) continue;
            if (readiness(s) < MIN_READY) continue;

            idle.add(s);
        }

        int reserve = (int) Math.floor(all.size() * RESERVE_FRACTION);
        int maxCommit = Math.min(MAX_STACKS_PER_OFFENSIVE, idle.size() - reserve);

        if (maxCommit < 1) return null;

        Map<Country, Set<Tile>> neighbors = server.getTileManager().getNeighbors(me);

        Operation best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Map.Entry<Country, Set<Tile>> entry : neighbors.entrySet()) {
            if (!server.getWarManager().isAtWar(me, entry.getKey())) continue;

            for (Tile target : entry.getValue()) {
                Tile staging = pickStaging(me, target, idle);
                if (staging == null) continue;

                double defenders = defenderPower(me, target);
                boolean defended = defenders > 0;

                List<TroopStack> commit = pickStacks(idle, staging, maxCommit, defended ? defenders * ATTACK_RATIO : 0);

                if (commit == null) continue;
                if (defended && commit.size() < MIN_STACKS_VS_DEFENDED_TILE) continue;

                double score = score(me, target, defenders, staging, commit);

                if (score > bestScore) {
                    bestScore = score;
                    best = new Operation(target, staging, new HashSet<>(commit), now);
                }
            }
        }

        return best;
    }

    private Tile pickStaging(Country me, Tile target, List<TroopStack> idle) {
        Tile best = null;
        double bestDist = Double.MAX_VALUE;

        Point stackCenter = averageCenter(idle);

        for (Tile n : Server.getInstance().getTileManager().findNeighbors(target)) {
            if (n.getController() == null || !n.getController().getAbbreviation().equals(me.getAbbreviation())) continue;

            double d = centre(n).distance(stackCenter);

            if (d < bestDist) {
                bestDist = d;
                best = n;
            }
        }

        return best;
    }

    private List<TroopStack> pickStacks(List<TroopStack> idle, Tile staging, int maxCommit, double requiredPower) {
        Point stagingCenter = centre(staging);

        List<TroopStack> sorted = new ArrayList<>(idle);
        sorted.sort(Comparator.comparingDouble(s -> centre(s.getTile()).distance(stagingCenter)));

        List<TroopStack> picked = new ArrayList<>();
        double sum = 0;

        for (TroopStack s : sorted) {
            if (picked.size() >= maxCommit) break;

            picked.add(s);
            sum += power(s);

            if (sum >= requiredPower) return picked;
        }

        return null;
    }

    private double score(Country me, Tile target, double defenders, Tile staging, List<TroopStack> commit) {
        double score = target.getVictoryPoints() * VP_SCORE;

        if (target.hasCity()) score += CITY_SCORE;

        if (target.getCores().contains(me.getAbbreviation()) || target.getClaims().contains(me.getAbbreviation())) score += CLAIM_CORE_SCORE;

        score -= defenders * SCORE_DEF;

        Point stagingCenter = centre(staging);

        double avgDist = commit.stream().mapToDouble(s -> centre(s.getTile()).distance(stagingCenter)).average().orElse(0);
        score -= avgDist * DISTANCE_SCORE;

        return score;
    }

    private double defenderPower(Country me, Tile target) {
        Server server = Server.getInstance();

        double power = 0;

        for (TroopStack s : server.getTroopManager().getAt(target)) if (isEnemyOf(me, s)) power += power(s);

        for (Tile n : server.getTileManager().findNeighbors(target)) {
            if (n.equals(target)) continue;

            double adj = 0;

            for (TroopStack s : server.getTroopManager().getAt(n)) if (isEnemyOf(me, s)) adj += power(s);

            power += adj * DEF_WEIGHT;
        }

        return power;
    }

    private boolean isEnemyOf(Country me, TroopStack s) {
        return s.getController() != null && Server.getInstance().getWarManager().isAtWar(me, s.getController());
    }

    private static double readiness(TroopStack s) {
        int n = s.getTroops().size();
        if (n == 0) return 0;

        return (hpFrac(s) + orgFrac(s)) / 2.0;
    }

    private static double hpFrac(TroopStack s) {
        int n = s.getTroops().size();
        return n == 0 ? 0 : clmp(s.getHp() / (n * MAX_HP_PER_TROOP));
    }

    private static double orgFrac(TroopStack s) {
        int n = s.getTroops().size();
        return n == 0 ? 0 : clmp(s.getOrg() / (n * MAX_ORG_PER_TROOP));
    }

    private static double power(TroopStack s) {
        return (s.getAttack() + s.getDefense()) * hpFrac(s) * (0.5 + 0.5 * orgFrac(s));
    }

    private static double clmp(double v) {
        return Math.clamp(v, 0, 1);
    }

    private Point centre(Tile tile) {
        return centres.computeIfAbsent(tile, t -> {
            List<Point> points = t.getPoints();
            if (points == null || points.isEmpty()) return new Point(0, 0);

            long x = 0, y = 0;

            for (Point p : points) {
                x += p.x;
                y += p.y;
            }

            return new Point((int) (x / points.size()), (int) (y / points.size()));
        });
    }

    private Point averageCenter(List<TroopStack> stacks) {
        if (stacks.isEmpty()) return new Point(0, 0);

        long x = 0, y = 0;

        for (TroopStack s : stacks) {
            Point c = centre(s.getTile());
            x += c.x;
            y += c.y;
        }

        return new Point((int) (x / stacks.size()), (int) (y / stacks.size()));
    }

    private record Operation(Tile target, Tile staging, Set<TroopStack> stacks, long startedAt) { }
}