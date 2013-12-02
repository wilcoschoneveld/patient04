/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package patient04.level;

import patient04.lighting.Renderer;
import patient04.math.Matrix;
import patient04.math.Vector;
import patient04.physics.AABB;
import patient04.resources.Model;

/**
 *
 * @author Wilco
 */
public class Solid {
    public Vector position;
    public Vector rotation;
    public Vector scale;
    
    public AABB aabb;
    
    public Model model;
    
    public Solid() {
        position = new Vector();
        rotation = new Vector();
        scale = new Vector(1, 1, 1);
    }
    
    public void draw(Renderer renderer) {
        // If there is no model, return
        if(model == null) return;
        
        // Create a new model matrix
        Matrix matrix = new Matrix();

        // Translate
        if(position != null && !position.isNull())
            matrix.translate(position.x, position.y, position.z);

        // Rotate
        if(rotation != null && !rotation.isNull()) {
            matrix.rotate(rotation.x, 1, 0, 0);
            matrix.rotate(rotation.y, 0, 1, 0);
            matrix.rotate(rotation.z, 0, 0, 1);
        }
        
        // Scale
        if(scale != null)
            matrix.scale(scale.x, scale.y, scale.z);
        
        // Update modelview matrix
        renderer.model = matrix;
        renderer.updateModelView();
        
        // Draw model
        model.draw();
    }
}
