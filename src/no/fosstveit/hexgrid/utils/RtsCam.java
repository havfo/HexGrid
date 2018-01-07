package no.fosstveit.hexgrid.utils;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bounding.BoundingVolume;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;


/**
 *
 * https://hub.jmonkeyengine.org/t/rts-camera-control/14761/27
 * 
 * @author abies
 */
public class RtsCam extends AbstractAppState {

    /**
     * Degree of Freedom
     * 
     */
    public enum DoF {
        SIDE,
        FWD,
        ROTATE,
        TILT,
        DISTANCE
    }

    public enum UpVector {

        Y_UP(Vector3f.UNIT_Y),
        Z_UP(Vector3f.UNIT_Z);

        final Vector3f upVector;

        UpVector(Vector3f upVector) {
            this.upVector = upVector;
        }

    }

    interface HeightProvider {
        public float getHeight(Vector2f coord);
    }

    private InputManager inputManager;
    private Camera cam;
    private BoundingVolume centerBounds;
    private BoundingVolume cameraBounds;

    private final int[] direction = new int[5];
    private final float[] accelTime = new float[5];
    private final float[] offsetMoves = new float[5];

    private final float[] maxSpeedPerSecondOfAccell = new float[5];
    private final float[] maxAccellPeriod = new float[5];
    private final float[] decelerationFactor = new float[5];
    private final float[] minValue = new float[5];
    private final float[] maxValue = new float[5];

    private final Vector3f position = new Vector3f();
    private final Vector3f center = new Vector3f();
    
    private final InternalListener listener = new InternalListener();

    private final UpVector up;

    private final Vector3f oldPosition = new Vector3f();
    private final Vector3f oldCenter = new Vector3f();
    private final Vector2f tempVec2 = new Vector2f();
    private float tilt = FastMath.PI / 4;
    private float rot = -FastMath.PI;
    private float distance = 10;
    
    private HeightProvider heightProvider;


    private boolean wheelEnabled = true;
    private String mouseRotationButton = "BUTTON2";
    private String mouseDragButton = "BUTTON3XXX";

    private boolean mouseRotation;
    private boolean mouseDrag;

    private static final int SIDE = DoF.SIDE.ordinal();
    private static final int FWD = DoF.FWD.ordinal();
    private static final int ROTATE = DoF.ROTATE.ordinal();
    private static final int TILT = DoF.TILT.ordinal();
    private static final int DISTANCE = DoF.DISTANCE.ordinal();

    private static final float WHEEL_SPEED = 1f / 15;

    private static String[] mappings = new String[] {
            "+SIDE", "+FWD", "+ROTATE", "+TILT", "+DISTANCE", "-SIDE", "-FWD", "-ROTATE", "-TILT", "-DISTANCE", "+WHEEL", "-WHEEL", "-MOUSEX", "+MOUSEX", "-MOUSEY", "+MOUSEY",
            "BUTTON1", "BUTTON2", "BUTTON3" };

    public RtsCam(UpVector up) {
        this.up = up;

        setMinMaxValues(DoF.SIDE, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        setMinMaxValues(DoF.FWD, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        setMinMaxValues(DoF.ROTATE, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        setMinMaxValues(DoF.TILT, 0.2f, (float) (Math.PI / 2) - 0.001f);
        setMinMaxValues(DoF.DISTANCE, 2, Float.POSITIVE_INFINITY);

        setMaxSpeed(DoF.SIDE, 10f, 0.4f);
        setMaxSpeed(DoF.FWD, 10f, 0.4f);
        setMaxSpeed(DoF.ROTATE, 2f, 0.4f);
        setMaxSpeed(DoF.TILT, 1f, 0.4f);
        setMaxSpeed(DoF.DISTANCE, 15f, 0.4f);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.cam = app.getCamera();
        this.inputManager = app.getInputManager();
        registerWithInput(inputManager);
    }

    /**
     * Set the maximum speed for given direction of movement. For SIDE/FWD/DISTANCE it is in units/second, for ROTATE/TILT it is in radians/second. Deceleration time is assumed to be the same
     * as acceleration time.
     * 
     * @param deg
     *            degree of freedom for which to set the maximum speed
     * @param maxSpd
     *            maximum speed of movement in that direction
     * @param accelTime
     *            amount of time which is need to accelerate to full speed in seconds (has to be bigger than zero, values over half second feel very sluggish). Defaults are 0.4
     *            seconds
     */
    public void setMaxSpeed(DoF deg, float maxSpd, float accelTime) {
        setMaxSpeed(deg,maxSpd,accelTime,accelTime);
    }
    
    /**
     * Set the maximum speed for given direction of movement. For SIDE/FWD/DISTANCE it is in units/second, for ROTATE/TILT it is in radians/second. 
     * 
     * @param deg
     *            degree of freedom for which to set the maximum speed
     * @param maxSpd
     *            maximum speed of movement in that direction
     * @param accelTime
     *            amount of time which is need to accelerate to full speed in seconds (has to be bigger than zero, values over half second feel very sluggish). Defaults are 0.4
     *            seconds
     * @param decelerationTime
     *            amount of time in seconds which is needed to automatically decelerate (friction-like stopping) from maxSpd to full stop.
     */
    public void setMaxSpeed(DoF deg, float maxSpd, float accelTime, float decelerationTime) {
        maxSpeedPerSecondOfAccell[deg.ordinal()] = maxSpd / accelTime;
        maxAccellPeriod[deg.ordinal()] = accelTime;
        if ( decelerationTime < 0.00001) {
            decelerationTime = 0.00001f;
        }
        decelerationFactor[deg.ordinal()] = accelTime/decelerationTime; 
    }

    /**
     * Set the terrain following logic for camera. Camera position will not get under the value returned by the heightProvider. Please add some extra buffering here,
     * so camera will not clip the actual terrain - for example
     * 
     * <pre>
     * new HeightProvider() {
     *     &#064;Override
     *     public float getHeight(Vector2f coord) {
     *         return terrain.getHeight(coord) + 10;
     *     }
     * }
     * </pre>
     * 
     * @param heightProvider
     */
    public void setHeightProvider(HeightProvider heightProvider) {
        this.heightProvider = heightProvider;
    }

    /**
     * Enables/disabled wheel-zoom behaviour
     * Default is enabled
     */
    public void setWheelEnabled(boolean wheelEnabled) {
        this.wheelEnabled = wheelEnabled;
    }

    private String mouseButtonName(int button) {
        switch (button) {
            case MouseInput.BUTTON_LEFT:
                return "BUTTON1";

            case MouseInput.BUTTON_MIDDLE:
                return "BUTTON2";

            case MouseInput.BUTTON_RIGHT:
                return "BUTTON3";
            default:
                return null;
        }
    }

    /**
     * Use MouseInput.BUTTON_ constants to indicate which buttons should be used for rotation and dragging with mouse
     * Defaults are BUTTON_MIDDLE for rotation and BUTTON_RIGHT for dragging
     * Use -1 to disable given functionality
     * 
     * @param rotationButton
     *            button to hold to control TILT/ROTATION with mouse movements
     * @param dragButton
     *            button to hold to drag camera position around
     */
    public void setMouseDragging(int rotationButton, int dragButton) {
        mouseDragButton = mouseButtonName(dragButton);
        mouseRotationButton = mouseButtonName(rotationButton);
    }

    public void update(final float tpf) {

        for (int i = 0; i < direction.length; i++) {
            int dir = direction[i];
            switch (dir) {
                case -1:
                    accelTime[i] = clamp(-maxAccellPeriod[i], accelTime[i] - tpf, accelTime[i]);
                    break;
                case 0:
                    if (accelTime[i] != 0) {
                        double oldSpeed = accelTime[i];
                        if (accelTime[i] > 0) {
                            accelTime[i] -= tpf*decelerationFactor[i];
                        } else {
                            accelTime[i] += tpf*decelerationFactor[i];
                        }
                        if (oldSpeed * accelTime[i] < 0) {
                            accelTime[i] = 0;
                        }
                    }
                    break;
                case 1:
                    accelTime[i] = clamp(accelTime[i], accelTime[i] + tpf, maxAccellPeriod[i]);
                    break;
            }

        }

        float distanceChange = maxSpeedPerSecondOfAccell[DISTANCE] * accelTime[DISTANCE] * tpf;
        distance += distanceChange;
        distance += offsetMoves[DISTANCE];

        tilt += maxSpeedPerSecondOfAccell[TILT] * accelTime[TILT] * tpf + offsetMoves[TILT];
        rot += maxSpeedPerSecondOfAccell[ROTATE] * accelTime[ROTATE] * tpf + offsetMoves[ROTATE];

        distance = clamp(minValue[DISTANCE], distance, maxValue[DISTANCE]);
        rot = clamp(minValue[ROTATE], rot%(FastMath.PI*2), maxValue[ROTATE]);
        tilt = clamp(minValue[TILT], tilt, maxValue[TILT]);

        double offX = maxSpeedPerSecondOfAccell[SIDE] * accelTime[SIDE] * tpf + offsetMoves[SIDE];
        double offY = maxSpeedPerSecondOfAccell[FWD] * accelTime[FWD] * tpf + offsetMoves[FWD];

        float sinRot = FastMath.sin(rot);
        float cosRot = FastMath.cos(rot);
        float cosTilt = FastMath.cos(tilt);
        float sinTilt = FastMath.sin(tilt);
        
        
        center.x += offX * cosRot + offY * sinRot;
        if (up == UpVector.Y_UP) {
            center.z += offX * -sinRot + offY * cosRot;
        } else {
            center.y += offX * -sinRot + offY * cosRot;
        }

        if (centerBounds != null) {
            //TODO: clamp center to bounds
        }

        if (up == UpVector.Y_UP) {
            position.x = center.x + (float) (distance * cosTilt * sinRot);
            position.y = center.y + (float) (distance * sinTilt);
            position.z = center.z + (float) (distance * cosTilt * cosRot);
            if (heightProvider != null) {
                float h = heightProvider.getHeight(tempVec2.set(position.x,position.z));
                if (position.y < h) {
                    position.y = h;
                }
            }
        } else {
            position.x = center.x + (float) (distance * cosTilt * sinRot);
            position.y = center.y + (float) (distance * cosTilt * cosRot);
            position.z = center.z + (float) (distance * sinTilt);
            if (heightProvider != null) {
                float h = heightProvider.getHeight(tempVec2.set(position.x,position.y));
                if (position.z < h) {
                    position.z = h;
                }
            }
        }

        for (int i = 0; i < offsetMoves.length; i++) {
            offsetMoves[i] = 0;
        }

        if (oldPosition.equals(position) && oldCenter.equals(center)) {
            return;
        }
        
        if (cameraBounds != null) {
            //TODO: clamp position to bounds
        }

        cam.setLocation(position);
        cam.lookAt(center, up.upVector);

        oldPosition.set(position);
        oldCenter.set(center);
        
    }

    private static float clamp(float min, float value, float max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        } else {
            return value;
        }
    }

    public float getMaxSpeed(DoF dg) {
        return maxSpeedPerSecondOfAccell[dg.ordinal()];
    }

    public float getMinValue(DoF dg) {
        return minValue[dg.ordinal()];
    }

    public float getMaxValue(DoF dg) {
        return maxValue[dg.ordinal()];
    }

    /**
     * SIDE and FWD min/max values are ignored
     * 
     * @param dg
     * @param min
     * @param max
     */
    public void setMinMaxValues(DoF dg, float min, float max) {
        minValue[dg.ordinal()] = min;
        maxValue[dg.ordinal()] = max;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setCenter(Vector3f center) {
        this.center.set(center);
    }

    public Vector3f getCenter() {
        return center;
    }

    public float getDistance() {
        return distance;
    }

    public float getRot() {
        return rot;
    }

    public float getTilt() {
        return tilt;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public void setRot(float rot) {
        this.rot = rot;
    }

    public void setTilt(float tilt) {
        this.tilt = tilt;
    }

    public Camera getCamera() {
        return cam;
    }

    private void registerWithInput(InputManager inputManager) {
        this.inputManager = inputManager;

        if (up == UpVector.Y_UP) {
            inputManager.addMapping("-SIDE", new KeyTrigger(KeyInput.KEY_A));
            inputManager.addMapping("+SIDE", new KeyTrigger(KeyInput.KEY_D));
            inputManager.addMapping("+ROTATE", new KeyTrigger(KeyInput.KEY_Q));
            inputManager.addMapping("-ROTATE", new KeyTrigger(KeyInput.KEY_E));
        } else {
            inputManager.addMapping("+SIDE", new KeyTrigger(KeyInput.KEY_A));
            inputManager.addMapping("-SIDE", new KeyTrigger(KeyInput.KEY_D));
            inputManager.addMapping("-ROTATE", new KeyTrigger(KeyInput.KEY_Q));
            inputManager.addMapping("+ROTATE", new KeyTrigger(KeyInput.KEY_E));
        }

        inputManager.addMapping("+FWD", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("-FWD", new KeyTrigger(KeyInput.KEY_W));

        inputManager.addMapping("+TILT", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("-TILT", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("-DISTANCE", new KeyTrigger(KeyInput.KEY_Z));
        inputManager.addMapping("+DISTANCE", new KeyTrigger(KeyInput.KEY_X));

        inputManager.addMapping("-WHEEL", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping("+WHEEL", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

        inputManager.addMapping("-MOUSEX", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("+MOUSEX", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("-MOUSEY", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("+MOUSEY", new MouseAxisTrigger(MouseInput.AXIS_Y, true));

        inputManager.addMapping("BUTTON1", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("BUTTON2", new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));
        inputManager.addMapping("BUTTON3", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));

        inputManager.addListener(listener, mappings);
    }

    @Override
    public void cleanup() {
        super.cleanup();

        for (String mapping : mappings) {
            if (inputManager.hasMapping(mapping)) {
                inputManager.deleteMapping(mapping);
            }
        }
        inputManager.removeListener(listener);
    }

    private class InternalListener implements ActionListener, AnalogListener {

        public void onAction(String name, boolean isPressed, float tpf) {
            if (!isEnabled()) {
                return;
            }

            int press = isPressed ? 1 : 0;

            if (name.contains("WHEEL") || name.contains("MOUSE")) {
                return;
            }

            if (name.equals(mouseRotationButton)) {
                mouseRotation = isPressed;
                inputManager.setCursorVisible(!mouseDrag && !mouseRotation);
                return;
            }

            if (name.equals(mouseDragButton)) {
                mouseDrag = isPressed;
                inputManager.setCursorVisible(!mouseDrag && !mouseRotation);
                return;
            }

            char sign = name.charAt(0);
            if (sign == '-') {
                press = -press;
            } else if (sign != '+') {
                return;
            }

            DoF deg = DoF.valueOf(name.substring(1));
            direction[deg.ordinal()] = press;
        }

        @Override
        public void onAnalog(String name, float value, float tpf) {
            if (!isEnabled()) {
                return;
            }

            if (!name.contains("WHEEL") && !name.contains("MOUSE")) {
                return;
            }

            char sign = name.charAt(0);
            if (sign == '-') {
                value = -value;
            } else if (sign != '+') {
                return;
            }

            if (name.contains("WHEEL")) {
                if (!wheelEnabled) {
                    return;
                }
                float speed = maxSpeedPerSecondOfAccell[DISTANCE] * maxAccellPeriod[DISTANCE] * WHEEL_SPEED;
                offsetMoves[DISTANCE] += value * speed;
            } else if (name.contains("MOUSE")) {
                if (mouseRotation) {
                    int direction;
                    if (name.endsWith("X")) {
                        direction = ROTATE;
                        if ( up == UpVector.Z_UP ) {
                            value = -value;
                        }
                    } else {
                        direction = TILT;
                    }
                    offsetMoves[direction] += value;
                } else if (mouseDrag) {
                    int direction;
                    if (name.endsWith("X")) {
                        direction = SIDE;
                        if ( up == UpVector.Z_UP ) {
                            value = -value;
                        }
                    } else {
                        direction = FWD;
                        value = -value;
                    }
                    offsetMoves[direction] += value * maxSpeedPerSecondOfAccell[direction] * maxAccellPeriod[direction];
                }
            }

        }
    }

}