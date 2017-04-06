package technology.tabula;

import org.apache.pdfbox.pdmodel.font.PDFont;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



@SuppressWarnings("serial")
public class Cell extends RectangularTextContainer<TextChunk> {
    private boolean spanning;
    private boolean placeholder;
    private List<TextChunk> textElements;
    private PDFont font;
    private float fontSize;


    public Cell(float top, float left, float width, float height) {
        super(top, left, width, height);
        this.setPlaceholder(false);
        this.setSpanning(false);
        this.setTextElements(new ArrayList<TextChunk>());
    }
    
    public Cell(Point2D topLeft, Point2D bottomRight) {
        super((float) topLeft.getY(), (float) topLeft.getX(), (float) (bottomRight.getX() - topLeft.getX()), (float) (bottomRight.getY() - topLeft.getY()));
        this.setPlaceholder(false);
        this.setSpanning(false);
        this.setTextElements(new ArrayList<TextChunk>());
    }
    
    @Override
    public String getText(boolean useLineReturns) {
        if (this.textElements.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Collections.sort(this.textElements);
        double curTop = this.textElements.get(0).getTop();
        for (TextChunk tc: this.textElements) {
            if (useLineReturns && tc.getTop() > curTop) {
                //change from sb.append('\r'); to double space for better output on windows
                sb.append(' ');
                sb.append(' ');

            }
            sb.append(tc.getText());
            curTop = tc.getTop();
        }
        return sb.toString().trim();
    }


    public String getText() {
        return getText(true);
    }

    public boolean isSpanning() {
        return spanning;
    }

    public void setSpanning(boolean spanning) {
        this.spanning = spanning;
    }

    public boolean isPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(boolean placeholder) {
        this.placeholder = placeholder;
    }

    public List<TextChunk> getTextElements() {
        return textElements;
    }

    public void setTextElements(List<TextChunk> textElements) {
        this.textElements = textElements;
    }

    public PDFont getFont(){
        if (textElements.size() !=0) return this.textElements.get(0).getFont();
        else return null;

    }
    public float getFontSize(){
        return  this.textElements.get(0).getFontSize();
    }

    //todo implement setfont in Cell
    public void setFont(){
        this.font= this.textElements.get(0).getFont();
    }

    public void setFontSize(){
        this.fontSize= this.textElements.get(0).getFontSize();
    }


}
