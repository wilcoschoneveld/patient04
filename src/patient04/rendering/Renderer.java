package patient04.rendering;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.ARBFramebufferObject.*;
import static org.lwjgl.opengl.ARBTextureFloat.GL_RGBA16F_ARB;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import patient04.math.Matrix;
import patient04.math.Vector;
import patient04.resources.Model;

import patient04.resources.Texture;
import patient04.utilities.Buffers;
import patient04.utilities.Logger;
import patient04.utilities.Shaders;

/**
 * TODO List:
 * - Full screen
 * - Frustum culling
 * - Diffuse color to shader for models
 * - Modelview matrix from ENEMy to Entity?
 * 
 * @author Wilco
 */
public class Renderer {
    // Geometry textures attachment locations
    public static final int positionAttachment = GL_COLOR_ATTACHMENT0,
                            normalAttachment = GL_COLOR_ATTACHMENT1,
                            diffuseAttachment = GL_COLOR_ATTACHMENT2,
                            accumAttachment = GL_COLOR_ATTACHMENT3;
    
    // Static geometry pass draw buffer list
    public static final IntBuffer gPassDrawBuffers = Buffers.createIntBuffer(
            positionAttachment, normalAttachment, diffuseAttachment);
    
    // Geometry pass texture objects
    private final Texture
            positionTexture, normalTexture, diffuseTexture, accumTexture;

    // Geometry frame buffer object
    private final int geometryBuffer, depthStencilBuffer;
    
    // Shaders
    private final int geometryShader, screenShader,
                        lightingShader, stencilShader, debugShader,
                            lightP, lightC, lightI, lightR, attC, attL, attQ,
                                effShader0, effShader1, effShader2, effShader3, effShader4;
    
    private float sinEffShader, cosEffShader, EffectIntensity = 0.0f , dX = 0;
    public float  theEnd = 1.0f;
    private boolean crazy = true;
    
    // Effect shaders
    private final int effectShader;
    
    // Keep track of active shader program
    private int currentProgram = 0;
    
    // Full screen quad
    private final Model screenQuad;
    
    // Matrices
    public Matrix projection, view;
    
    public Renderer() {
        // Enable depth testing and backface culling
        glLoadDefaults();
        
        // Load full screen quad
        screenQuad = Model.getResource("lightDirectional.obj");
        
        // Enable client states
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        
        // Obtain display width and height for FBO
        int w = Display.getWidth(), h = Display.getHeight();
        
        // Create new attachable texture buffers
        positionTexture = new Texture(w, h, GL_RGBA16F_ARB);
        normalTexture = new Texture(w, h, GL_RGBA16F_ARB);
        diffuseTexture = new Texture(w, h, GL11.GL_RGBA8);
        accumTexture = new Texture(w, h, GL11.GL_RGBA8, null,
                                    GL11.GL_LINEAR, GL11.GL_LINEAR);
        
        // Create new attachable render buffer
        depthStencilBuffer = glGenRenderbuffers();
        
        // Set storage to depth component
        glBindRenderbuffer(GL_RENDERBUFFER, depthStencilBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, w, h);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
        
        // Create a new geometry FBO
        geometryBuffer = glGenFramebuffers();
        
        // Bind the FBO
        glBindFramebuffer(GL_FRAMEBUFFER, geometryBuffer);
        
        // Attach the texture buffers
        glFramebufferTexture2D(GL_FRAMEBUFFER,
                positionAttachment, GL11.GL_TEXTURE_2D, positionTexture.id, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER,
                normalAttachment, GL11.GL_TEXTURE_2D, normalTexture.id, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER,
                diffuseAttachment, GL11.GL_TEXTURE_2D, diffuseTexture.id, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER,
                accumAttachment, GL11.GL_TEXTURE_2D, accumTexture.id, 0);
        
        // Attach depth buffer
        glFramebufferRenderbuffer(GL_FRAMEBUFFER,
             GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depthStencilBuffer);
        
        // Check framebuffer status
        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            Logger.error("Framebuffer error!");
        
        // Unbind the FBO
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        
        // Load the geometry shader
        geometryShader = Shaders.loadShaderPairFromFiles(
                "res/shaders/geometry.vert", "res/shaders/geometry.frag");
        
        // Load the lighting shader
        lightingShader = Shaders.loadShaderPairFromFiles(
                "res/shaders/lighting.vert", "res/shaders/lighting.frag");
        
        // Bind the lighting shader
        useShaderProgram(lightingShader);
        
        // Obtain uniform variable locations
        lightP = GL20.glGetUniformLocation(lightingShader, "lightPosition");
        lightC = GL20.glGetUniformLocation(lightingShader, "lightColor");
        lightI = GL20.glGetUniformLocation(lightingShader, "lightIntensity");
        lightR = GL20.glGetUniformLocation(lightingShader, "lightRadius");
        
        attC = GL20.glGetUniformLocation(lightingShader, "falloffConstant");
        attL = GL20.glGetUniformLocation(lightingShader, "falloffLinear");
        attQ = GL20.glGetUniformLocation(lightingShader, "falloffQuadratic");
        
        // Set screensize
        Shaders.glUniform2f(lightingShader, "screenSize", w, h);
        
        // Set samplers to correct texture units
        Shaders.glUniform1i(lightingShader, "uTexPosition", 0);
        Shaders.glUniform1i(lightingShader, "uTexNormal", 1);
        Shaders.glUniform1i(lightingShader, "uTexDiffuse", 2);
        
        // Set stencil operations
        stencilShader = Shaders.loadShaderPairFromFiles(
                "res/shaders/lighting.vert", "res/shaders/empty.frag");
        
        useShaderProgram(stencilShader);
        
        GL20.glStencilOpSeparate(GL11.GL_FRONT,
                GL11.GL_KEEP, GL14.GL_INCR_WRAP, GL11.GL_KEEP);
        GL20.glStencilOpSeparate(GL11.GL_BACK,
                GL11.GL_KEEP, GL14.GL_DECR_WRAP, GL11.GL_KEEP);
        
        screenShader = Shaders.loadShaderPairFromFiles(
                "res/shaders/pass.vert", "res/shaders/ambient.frag");
        
        useShaderProgram(screenShader);
        
        Shaders.glUniform1i(screenShader, "uTexPosition", 0);
        Shaders.glUniform1i(screenShader, "uTexNormal", 1);
        Shaders.glUniform1i(screenShader, "uTexDiffuse", 2);
        Shaders.glUniform2f(screenShader, "screenSize", w, h);
        
        // Effect Shader
        effectShader = Shaders.loadShaderPairFromFiles(
                "res/shaders/pass.vert", "res/shaders/effect.frag");
        
        useShaderProgram(effectShader);
        
        effShader0 = GL20.glGetUniformLocation(effectShader, "sinEffShader");
        effShader1 = GL20.glGetUniformLocation(effectShader, "cosEffShader");         
        effShader2 = GL20.glGetUniformLocation(effectShader, "dX");
        effShader3 = GL20.glGetUniformLocation(effectShader, "EffectIntensity");
        effShader4 = GL20.glGetUniformLocation(effectShader, "theEnd");
        
        Shaders.glUniform1i(effectShader, "uTexAccum", 0);
        Shaders.glUniform2f(effectShader, "screenSize", w, h);
        
        // Load debug shader
        debugShader = Shaders.loadShaderPairFromFiles(
                "res/shaders/pass.vert", "res/shaders/gbuffer.frag");
        
        useShaderProgram(debugShader);
        
        // Bind uniform variables 
        Shaders.glUniform1i(debugShader, "uTexPosition", 0);
        Shaders.glUniform1i(debugShader, "uTexNormal", 1);
        Shaders.glUniform1i(debugShader, "uTexDiffuse", 2);
        Shaders.glUniform1i(debugShader, "uTexAccum", 3);
        Shaders.glUniform2f(debugShader, "screenSize", w, h);
        
        // Unbind shader program
        useShaderProgram(0);
        
        Logger.log("Renderer loaded");
    }
    
    public final void useShaderProgram(int program) {
        if(currentProgram != program) {
            GL20.glUseProgram(program);
            currentProgram = program;
        }
    }
    
    public void dispose() {
        // Delete the framebuffers
        glDeleteFramebuffers(geometryBuffer);
        glDeleteRenderbuffers(depthStencilBuffer);
        
        // Delete the textures
        positionTexture.dispose();
        normalTexture.dispose();
        diffuseTexture.dispose();
        accumTexture.dispose();
        
        // Delete the shaders
        GL20.glDeleteProgram(geometryShader);
        GL20.glDeleteProgram(lightingShader);
        GL20.glDeleteProgram(debugShader);
    }
    
    public void geometryPass() {
        // Bind the geometry frame buffer object
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, geometryBuffer);
        GL20.glDrawBuffers(gPassDrawBuffers);
        
        // Set OpenGL state
        glLoadDefaults();
        glUpdateProjectionMatrix();
        GL11.glDepthMask(true);
        
        // Clear the buffer
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        
        // Bind the geometry shader
        useShaderProgram(geometryShader);
        
    }
    
    public void lightingPass() {
        // Bind the window provided buffer object
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, geometryBuffer);
        GL11.glDrawBuffer(accumAttachment);
        
        // Set OpenGL state
        glLoadDefaults();
        glUpdateProjectionMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        
        // Clear the buffer
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        
        // Be nice and clear Texture cache
        Texture.unbind();
        
        // Bind the GBuffer textures to TEXTURE0,1,etc..
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, diffuseTexture.id);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, normalTexture.id);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, positionTexture.id);
        
        
        useShaderProgram(screenShader);
        
        screenQuad.draw();
        
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glCullFace(GL11.GL_FRONT);
    }
    
    public void guiPass(int timeWithoutMedicine) {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, accumTexture.id);
        
        // Set OpenGL state
        glLoadDefaults();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        
        useShaderProgram(effectShader);
        glUpdateEffectPrams(timeWithoutMedicine);
        
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        screenQuad.draw();
        
        // Bind the standard shader
        useShaderProgram(0);
        
    }
    
    public void debugPass() {
        // Bind the window provided buffer object
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        
        glLoadDefaults();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        
        useShaderProgram(debugShader);
        
        Texture.unbind();
        
        // Bind the GBuffer textures to TEXTURE0,1,etc..
        GL13.glActiveTexture(GL13.GL_TEXTURE3);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, accumTexture.id);
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, diffuseTexture.id);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, normalTexture.id);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, positionTexture.id);
        
        screenQuad.draw();
    }
    
    private void glUpdateProjectionMatrix() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadMatrix(projection.toBuffer());
    }
    
    public void glUpdateModelMatrix(Matrix model) {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        
        if(model != null)
            GL11.glLoadMatrix(view.copy().multiply(model).toBuffer());
        else
            GL11.glLoadMatrix(view.toBuffer());
    }
    
    public final void glLoadDefaults() {
        // Enable depth testing
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
                
        // Set back-face culling
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        
        // Disable blending
        GL11.glDisable(GL11.GL_BLEND);
        
        // Disable stencil
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }
    
    public void glUpdateEffectPrams(int timeWithoutMedicine){
        if(crazy == true){
            
            EffectIntensity = 0.45f;
            
            sinEffShader = (float) Math.sin(Math.toRadians(timeWithoutMedicine));
            cosEffShader = (float) Math.cos(Math.toRadians(timeWithoutMedicine) +90);
            dX = (float) Math.sin(Math.toRadians(timeWithoutMedicine));
                        
            GL20.glUniform1f(effShader0, sinEffShader);
            GL20.glUniform1f(effShader1, cosEffShader);
            GL20.glUniform1f(effShader2, dX);
            GL20.glUniform1f(effShader3, EffectIntensity);
            GL20.glUniform1f(effShader4, theEnd);
            
            // the modulus indicates how fast the effect increases and the smaller then 0.4 is the max intenisty of the effect;
            if(timeWithoutMedicine % 10 == 0 && EffectIntensity <= 0.4f){
                EffectIntensity += 0.01;
            }
            
            if( theEnd >0.0f && timeWithoutMedicine % 10 == 0 && EffectIntensity >= 0.4f){
                theEnd-= 0.01f;
                if(theEnd <=0.2){
                    // game over
                    System.out.println("GameOver");
                }
            }
        }
    }
    public void glSetCrazy(boolean setCrazy){
        crazy = setCrazy;
    }
    
    public void glResetEffectIntensity(){
        EffectIntensity = 0.0f;
    }
    
    public void glUpdateLightParams(Light light) {
        // Upload light position
        Vector pos = light.getPosition().copy().premultiply(view);
        
        // Update light values
        GL20.glUniform3f(lightP, pos.x, pos.y, pos.z);
        GL20.glUniform4(lightC, light.getColor());
        GL20.glUniform1f(lightI, light.getIntensity());
        GL20.glUniform1f(lightR, light.getRadius());
        
        // Update light attenuation model
        GL20.glUniform1f(attC, light.getConstant());
        GL20.glUniform1f(attL, light.getLinear());
        GL20.glUniform1f(attQ, light.getQuadratic());
    }
    
    public void pointLightFirstPass() {
        useShaderProgram(stencilShader);
        
        GL11.glDrawBuffer(0);
        
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0);
        
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
    }
    
    public void pointLightSecondPass() {
        useShaderProgram(lightingShader);
        
        GL11.glDrawBuffer(accumAttachment);
        
        GL11.glStencilFunc(GL11.GL_NOTEQUAL, 0, 0xFF);
        
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }
}
