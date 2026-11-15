package de.idiotischer.bob.player.brain.impl;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.player.brain.BrainI;
import de.idiotischer.bob.player.tasks.Task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class BasicBrain extends BrainI {

    private final List<Task> tasks = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    private volatile Task currentTask;

    public void registerTask(Task task) {
        tasks.add(task);
    }

    @Override
    public Task getNextTask() {
        return tasks.stream().filter(task -> task.canExecute(country)).max(Comparator.comparingDouble(task -> task.getPriority(country))).orElse(null);
    }

    @Override
    public Task getPreviousTask() {
        return null;
    }

    @Override
    public Task getCurrentTask() {
        return currentTask;
    }

    @Override
    public void tick() {
        if (!lock.tryLock()) return;

        try {
            Task bestTask = getNextTask();

            if (bestTask != currentTask) {
                if (currentTask != null) currentTask.onStop(country);

                currentTask = bestTask;

                if (currentTask != null) currentTask.onStart(country);
            }

            Task task = currentTask;

            if (task != null) {
                task.tick(country);

                if (task.finished(country)) {
                    task.onStop(country);
                    currentTask = null;
                }
            }
        } finally {
            lock.unlock();
        }
    }
}