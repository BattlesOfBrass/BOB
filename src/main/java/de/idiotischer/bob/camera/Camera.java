package de.idiotischer.bob.camera;

import org.jetbrains.annotations.ApiStatus;

import java.awt.geom.AffineTransform;

public class Camera {

    private double x = 0;
    private double y = 0;
    private double zoom = 1.0;

    private final int mapWidth;
    private final int mapHeight;

    private int viewportWidth;
    private int viewportHeight;
    private boolean wasAtMinZoom;

    public Camera(int mapWidth, int mapHeight) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
    }

    //public void setViewportSize(int width, int height) {
    //    this.viewportWidth = width;
    //    this.viewportHeight = height;
    //    clamp();
    //}

    public void move(double dx, double dy, boolean clamp) {
        x += dx;
        y += dy;
        if(clamp) clamp();
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        clamp();
    }

    public void zoomToMin(int red) {
        zoom = getMinZoom() - red;
        clamp();
    }

    public void zoomToMin() {
        clamp();
        zoom = getMinZoom();
        clamp();
    }

    public void zoomToMin(double centerX, double centerY) {
        double worldX = (centerX + x) / zoom;
        double worldY = (centerY + y) / zoom;

        zoom = getMinZoom();

        x = worldX * zoom - centerX;
        y = worldY * zoom - centerY;

        clamp();
    }

    public void setViewportSize(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;

        double newMinZoom = getMinZoom();

        if (wasAtMinZoom) {
            this.zoom = newMinZoom;
            this.x = 0;
            this.y = 0;
        } else {
            this.zoom = Math.max(newMinZoom, this.zoom);
        }

        clamp();
    }

    public void zoom(double factor, double pivotX, double pivotY) {
        double worldX = (pivotX + x) / zoom;
        double worldY = (pivotY + y) / zoom;

        zoom *= factor;

        double min = getMinZoom();
        zoom = Math.max(min, Math.min(zoom, 20));

        x = worldX * zoom - pivotX;
        y = worldY * zoom - pivotY;

        wasAtMinZoom = (Math.abs(zoom - min) < 0.001);

        clamp();
    }

    public void clamp() {
        double scaledWidth = mapWidth * zoom;
        double scaledHeight = mapHeight * zoom;

        if (scaledWidth > viewportWidth) {
            x = Math.max(0, Math.min(x, scaledWidth - viewportWidth));
        } else {
            x = (scaledWidth - viewportWidth) / 2.0;
        }

        if (scaledHeight > viewportHeight) {
            y = Math.max(0, Math.min(y, scaledHeight - viewportHeight));
        } else {
            y = (scaledHeight - viewportHeight) / 2.0;
        }
    }

    //    public void clampOffsets() {
    //        int panelWidth = renderPanel.getWidth();
    //        int panelHeight = renderPanel.getHeight();
    //
    //        int mapWidth = map.getWidth();
    //        int mapHeight = map.getHeight();
    //
    //        double scaledWidth = mapWidth * zoom;
    //        double scaledHeight = mapHeight * zoom;
    //
    //        if (scaledWidth <= panelWidth) {
    //            offsetX = -(panelWidth - scaledWidth) / 2;
    //        } else {
    //            offsetX = Math.max(0, Math.min(offsetX, scaledWidth - panelWidth));
    //        }
    //
    //        if (scaledHeight <= panelHeight) {
    //            offsetY = -(panelHeight - scaledHeight) / 2;
    //        } else {
    //            offsetY = Math.max(0, Math.min(offsetY, scaledHeight - panelHeight));
    //        }
    //    }

    public double getMinZoom() {
        if (viewportWidth <= 0 || viewportHeight <= 0) return 1.0;
        return Math.min((double) viewportWidth / mapWidth, (double) viewportHeight / mapHeight);
    }

    @ApiStatus.Obsolete
    public void forceFit() {
        this.zoom = getMinZoom();
        this.x = 0;
        this.y = 0;
        this.wasAtMinZoom = true;
        clamp();
    }

    public AffineTransform getTransform() {
        AffineTransform at = new AffineTransform();
        //at.translate(-Math.round(x), -Math.round(y));
        at.translate(-x, -y);
        at.scale(zoom, zoom);
        return at;
    }

    public int screenToWorldX(int screenX) {
        return (int) ((screenX + x) / zoom);
    }

    public int screenToWorldY(int screenY) {
        return (int) ((screenY + y) / zoom);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZoom() { return zoom; }

    public int getMapWidth() {
        return mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }
}