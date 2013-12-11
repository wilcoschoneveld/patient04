/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package patient04.editor;

import java.util.Locale;
import org.lwjgl.input.Mouse;
import patient04.resources.Font;
import patient04.states.Editor;

/**
 *
 * @author Wilco
 */
public class Info {
    public static final float AXIS_ARROW_LENGTH = 100f;
    public static final float AXIS_ARROW_HEAD = 5f;
    
    private final Editor editor;
    private final Font fntInfo;
    
    public Info(Editor editor) {
        this.editor = editor;
        
        fntInfo = Font.getResource("Verdana", Font.BOLD, 15);
    }
    
    public void draw() {
        fntInfo.draw(0.02f, 0.02f, "Mouse location: " +
                String.format(Locale.US, "x = %.4f, z = %.4f",
                        editor.camera.convertMouseX(Mouse.getX()),
                        editor.camera.convertMouseY(Mouse.getY())));
    }
}
