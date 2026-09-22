package com.mpp.stellaeomphalos.client.view;

/** A camera capability. State transitions are owned by AbstractViewSequence. */
public interface CameraController {
    int priority();

    boolean finished();

    void tick();

    void sample(float partial, org.joml.Vector3d position);

    float yaw();

    float pitch();

    float fov();

    void stop();
}
