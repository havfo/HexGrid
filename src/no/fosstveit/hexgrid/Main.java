package no.fosstveit.hexgrid;

import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.TechniqueDef.LightMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.CartoonEdgeFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.texture.Texture2D;
import com.jme3.water.WaterFilter;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import no.fosstveit.hexgrid.hexmap.HexMap;
import no.fosstveit.hexgrid.hexmap.HexGridChunk;
import no.fosstveit.hexgrid.hexmap.HexOutline;
import no.fosstveit.hexgrid.utils.RtsCam;
import no.fosstveit.hexgrid.utils.RtsCam.DoF;
import no.fosstveit.hexgrid.utils.RtsCam.UpVector;

/**
 * Example usage of HexGrid
 *
 * @author Håvar Aambø Fosstveit
 */
public class Main extends SimpleApplication {

    private final int SHADOWMAP_SIZE = 512;
    private DirectionalLightShadowRenderer dlsr;
    private DirectionalLightShadowFilter dlsf;
    private Geometry mark;
    private HexOutline outline;
    private Node clickable;
    private HexMap hg;
    private final Vector3f lightDir = new Vector3f(40f, -15f, 20f);
    private WaterFilter water;
    private final float initialWaterHeight = 0.8f;
    private boolean wireframe = false;
    private boolean showGrid = false;
    private Material mat;
    private static int width = 1440;
    private static int height = 900;
    private Vector2f clickPos;

    private final RtsCam rtsCam = new RtsCam(UpVector.Y_UP);

    public static void main(String[] args) {
        Main app = new Main();

        AppSettings cfg = new AppSettings(true);
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode[] modes = device.getDisplayModes();

        int camIndex = 0;
        for (int j = 0; j < (modes.length - 1); j++) {
            if (modes[j].getHeight() >= 576) {
                // Found first camera settings thats over 576 in height.
                camIndex = j;
                break;
            }
        }

        // cfg.setResolution(modes[camIndex].getWidth(), modes[camIndex].getHeight());
        cfg.setResolution(width, height);
        // cfg.setFrequency(modes[camIndex].getRefreshRate());
        // cfg.setBitsPerPixel(modes[camIndex].getBitDepth());

        cfg.setVSync(true); // prevents page tearing
        cfg.setFullscreen(false);
        cfg.setSamples(0); // anti-aliasing
        cfg.setTitle("HexGrid"); // branding: window name

        /* try {
            // Branding: window icon
            cfg.setIcons(new BufferedImage[]{ImageIO.read(new File("assets/Interface/icon.gif"))});
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Icon missing.", ex);
        } */
        // branding: load splashscreen from assets
        // cfg.setSettingsDialogImage("Interface/MySplashscreen.png");
        app.setShowSettings(true); // or don't display splashscreen
        app.setSettings(cfg);

        app.start();
    }

    @Override
    public void simpleInitApp() {
        initKeys();
        initMark();

        clickable = new Node("Clickable objects");

        mat = new Material(assetManager, "Shaders/HexGridLighting.j3md");
        mat.setColor("Diffuse", ColorRGBA.White);
        // mat.setColor("Specular", ColorRGBA.White);
        // mat.setBoolean("UseMaterialColors", true);
        // mat.setColor("Ambient", ColorRGBA.Black);
        // mat.setFloat("Shininess", 0.1f);

        List<Image> images = new ArrayList<>();
        images.add(assetManager.loadTexture("Textures/grass.png").getImage());
        images.add(assetManager.loadTexture("Textures/mud.png").getImage());
        images.add(assetManager.loadTexture("Textures/sand.png").getImage());
        images.add(assetManager.loadTexture("Textures/snow.png").getImage());
        images.add(assetManager.loadTexture("Textures/stone.png").getImage());
        images.add(assetManager.loadTexture("Textures/plain.png").getImage());
        images.add(assetManager.loadTexture("Textures/jungle.png").getImage());
        images.add(assetManager.loadTexture("Textures/hill.png").getImage());
        TextureArray texArr = new TextureArray(images);
        texArr.setMinFilter(Texture.MinFilter.Trilinear);
        texArr.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("ColorMap", texArr);

        Texture gridLines = assetManager.loadTexture("Textures/grid.png");
        gridLines.setMinFilter(Texture.MinFilter.Trilinear);
        gridLines.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("GridLines", gridLines);

        hg = new HexMap(32, 20);
        for (HexGridChunk chunk : hg.getChunks()) {
            Geometry geom = new Geometry("HexGridChunk", chunk);
            geom.setShadowMode(ShadowMode.CastAndReceive);
            geom.setCullHint(Spatial.CullHint.Dynamic);
            // TangentBinormalGenerator.generate(chunk);

            geom.setMaterial(mat);
            clickable.attachChild(geom);
        }

        rootNode.attachChild(clickable);

        /* Geometry wire = geom.clone();
        Material wiremat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

        wiremat.setColor("Color", ColorRGBA.Black);
        wiremat.getAdditionalRenderState().setWireframe(true);
        wire.setMaterial(wiremat);
        rootNode.attachChild(wire); */
        // setDisplayStatView(false);
        // setDisplayFps(false);
        getStateManager().detach(getStateManager().getState(FlyCamAppState.class));
        rtsCam.setCenter(new Vector3f(50, 25, 0));
        rtsCam.setDistance(50);
        rtsCam.setMaxSpeed(DoF.FWD, 100, 0.05f, 0.05f);
        rtsCam.setMaxSpeed(DoF.SIDE, 100, 0.05f, 0.05f);
        rtsCam.setMaxSpeed(DoF.DISTANCE, 100, 0.05f, 0.05f);
        rtsCam.setMaxSpeed(DoF.ROTATE, 5, 0.05f, 0.05f);
        rtsCam.setMouseDragging(MouseInput.BUTTON_RIGHT, MouseInput.BUTTON_LEFT);
        rtsCam.setMinMaxValues(DoF.DISTANCE, 10, 500);
        getStateManager().attach(rtsCam);

        /* SimpleWaterProcessor waterProcessor = new SimpleWaterProcessor(assetManager);
        waterProcessor.setReflectionScene(clickable);
        // we set the water plane
        Vector3f waterLocation = new Vector3f(0f, 1f, 0f);
        waterProcessor.setPlane(new Plane(Vector3f.UNIT_Y, waterLocation.dot(Vector3f.UNIT_Y)));
        viewPort.addProcessor(waterProcessor);

        // we set wave properties
        waterProcessor.setWaterDepth(50);         // transparency of water
        waterProcessor.setDistortionScale(0.05f); // strength of waves
        waterProcessor.setWaveSpeed(0.01f);       // speed of waves
        waterProcessor.setWaterColor(ColorRGBA.Blue);
        // waterProcessor.setReflectionClippingOffset(1.8f);

        // we define the wave size by setting the size of the texture coordinates
        Quad quad = new Quad(1000, 1000);
        quad.scaleTextureCoordinates(new Vector2f(20f, 20f));

        // we create the water geometry from the quad
        Geometry water = new Geometry("water", quad);
        water.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        water.setLocalTranslation(-500f, 0.5f, 500f);
        // water.setShadowMode(ShadowMode.CastAndReceive);
        water.setMaterial(waterProcessor.getMaterial());
        rootNode.attachChild(water);
        // clickable.attachChild(water);
        */
        
        
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(lightDir);
        sun.setColor(ColorRGBA.White.mult(0.7f));
        rootNode.addLight(sun);

        renderManager.setPreferredLightMode(LightMode.MultiPass);
        // renderManager.setSinglePassLightBatchSize(2);

        Arrow arrow = new Arrow(lightDir);
        putShape(arrow, ColorRGBA.Yellow).setLocalTranslation(new Vector3f(0f, 50f, 0f));

        AmbientLight ambientLight = new AmbientLight();
        ambientLight.setColor(ColorRGBA.White.mult(0.5f));
        rootNode.addLight(ambientLight);

        dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr.setLight(sun);
        dlsr.setLambda(0.4f);
        dlsr.setShadowIntensity(0.5f);
        dlsr.setEdgeFilteringMode(EdgeFilteringMode.Bilinear);
        // dlsr.displayDebug();
        viewPort.addProcessor(dlsr);

        //Water Filter
        water = new WaterFilter(rootNode, lightDir);
        water.setWaterColor(new ColorRGBA().setAsSrgb(0.1f, 0.7f, 0.85f, 1.0f));
        water.setDeepWaterColor(new ColorRGBA().setAsSrgb(0.0039f, 0.00196f, 0.145f, 1.0f));
        water.setUnderWaterFogDistance(80);
        water.setWaterTransparency(1.5f);
        water.setFoamIntensity(0.2f);
        water.setFoamHardness(0.3f);
        water.setFoamExistence(new Vector3f(0.5f, 5f, 0.5f));
        water.setReflectionDisplace(10);
        water.setRefractionConstant(0.9f);
        water.setColorExtinction(new Vector3f(30, 50, 70));
        water.setSpeed(0.2f);
        water.setShoreHardness(2f);
        water.setShininess(80f);
        water.setCausticsIntensity(0.9f);
        water.setWaveScale(0.001f);
        water.setMaxAmplitude(0.1f);
        water.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam.jpg"));
        water.setRefractionStrength(0.2f);
        water.setWaterHeight(initialWaterHeight);

        //Bloom Filter
        // BloomFilter bloom = new BloomFilter();
        // bloom.setExposurePower(55);
        // bloom.setBloomIntensity(1.0f);
        //Light Scattering Filter
        // LightScatteringFilter lsf = new LightScatteringFilter(lightDir.mult(-300));
        // lsf.setLightDensity(1.4f);
        //Depth of field Filter
        // DepthOfFieldFilter dof = new DepthOfFieldFilter();
        // dof.setFocusDistance(0);
        // dof.setFocusRange(100);
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);

        // SSAOFilter ssaoFilter = new SSAOFilter(10f, 2f, 6f, 0.1f);;
        // fpp.addFilter(ssaoFilter);
        fpp.addFilter(water);
        // fpp.addFilter(bloom);
        // fpp.addFilter(dof);
        // fpp.addFilter(lsf);
        // fpp.addFilter(new FXAAFilter());
        // fpp.addFilter(new CartoonEdgeFilter());

        // int numSamples = getContext().getSettings().getSamples();
        // if (numSamples > 0) {
        //     fpp.setNumSamples(numSamples);
        // }

        viewPort.addProcessor(fpp);

        attachCoordinateAxes(new Vector3f(0, 10, 0));
    }

    private void initKeys() {
        inputManager.addMapping("Click", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "Click");
        inputManager.addRawInputListener(rawActionListener);
        inputManager.addMapping("toggle wireframe", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addListener(actionListener, "toggle wireframe");
        inputManager.addMapping("toggle grid", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addListener(actionListener, "toggle grid");
    }

    private RawInputListener rawActionListener = new RawInputListener() {
        @Override
        public void beginInput() {
        }

        @Override
        public void endInput() {
        }

        @Override
        public void onJoyAxisEvent(JoyAxisEvent evt) {
        }

        @Override
        public void onJoyButtonEvent(JoyButtonEvent evt) {
        }

        @Override
        public void onMouseMotionEvent(MouseMotionEvent evt) {
        }

        @Override
        public void onMouseButtonEvent(MouseButtonEvent evt) {
        }

        @Override
        public void onKeyEvent(KeyInputEvent evt) {
        }

        @Override
        public void onTouchEvent(TouchEvent evt) {
        }
    };

    private ActionListener actionListener = new ActionListener() {

        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("Click") && !keyPressed) {
                CollisionResults results = new CollisionResults();
                Vector2f click2d = inputManager.getCursorPosition();
                Vector3f click3d = cam.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 0f).clone();
                Vector3f dir = cam.getWorldCoordinates(new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d).normalizeLocal();
                Ray ray = new Ray(click3d, dir);
                clickable.collideWith(ray, results);
                // System.out.println("----- Collisions? " + results.size() + "-----");
                for (int i = 0; i < results.size(); i++) {
                    // float dist = results.getCollision(i).getDistance();
                    Vector3f pt = results.getCollision(i).getContactPoint();
                    // String hit = results.getCollision(i).getGeometry().getName();
                    // System.out.println("* Collision #" + i);
                    // System.out.println("  You clicked " + hit + " at " + pt + ", " + dist + " wu away.");
                    // System.out.println("  You hit cell: " + hg.getCell(pt));
                    outline.setCell(hg.getCell(pt));
                    outline.triangulate();
                    break;
                }

                if (results.size() > 0) {
                    // CollisionResult closest = results.getClosestCollision();
                    // mark.setLocalTranslation(closest.getContactPoint());
                    rootNode.attachChild(mark);
                } else {
                    rootNode.detachChild(mark);
                }
            } else if (name.equals("toggle wireframe") && !keyPressed) {
                wireframe = !wireframe; // toggle boolean
                mat.getAdditionalRenderState().setWireframe(wireframe);
            } else if (name.equals("toggle grid") && !keyPressed) {
                showGrid = !showGrid;
                mat.setBoolean("ShowGrid", showGrid);
            }
        }
    };

    protected void initMark() {
        outline = new HexOutline();
        mark = new Geometry("Clicked!", outline);
        mark.setShadowMode(ShadowMode.Receive);
        mark.setCullHint(Spatial.CullHint.Never);
        Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mark_mat.setColor("Color", ColorRGBA.Red);
        mark.setMaterial(mark_mat);
    }

    private void attachCoordinateAxes(Vector3f pos) {
        Arrow arrow = new Arrow(new Vector3f(20f, 0f, 0f));
        putShape(arrow, ColorRGBA.Red).setLocalTranslation(pos);

        arrow = new Arrow(new Vector3f(0f, 20f, 0f));
        putShape(arrow, ColorRGBA.Green).setLocalTranslation(pos);

        arrow = new Arrow(new Vector3f(0f, 0f, 20f));
        putShape(arrow, ColorRGBA.Blue).setLocalTranslation(pos);
    }

    private Geometry putShape(Mesh shape, ColorRGBA color) {
        Geometry g = new Geometry("coordinate axis", shape);
        Material matar = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matar.getAdditionalRenderState().setWireframe(true);
        matar.getAdditionalRenderState().setLineWidth(4);
        matar.setColor("Color", color);
        g.setMaterial(matar);
        rootNode.attachChild(g);
        return g;
    }

    @Override
    public void simpleUpdate(float tpf) {
        Vector2f position = inputManager.getCursorPosition();
        float x = position.getX();
        float y = position.getY();

        if (x <= 35) { // At left edge of screen

        } else if (x >= width - 35) { // At right edge of screen

        }

        if (y <= 35) { // At bottom edge of screen

        } else if (y >= height - 35) { // At top edge of screen

        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
}