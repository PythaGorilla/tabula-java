package technology.tabula;

import org.apache.pdfbox.pdmodel.font.PDFont;

import java.util.List;
import java.util.Optional;
@SuppressWarnings("serial")
public abstract class RectangularTextContainer<T extends HasText> extends Rectangle {

     //Java so fucking silly two new new
    private Table.CellPosition cell_position = new Table().new CellPosition(0,0);

    public void setCell_position(int r, int c){
        this.cell_position.row = r;
        this.cell_position.col =c;
        }
    public Table.CellPosition getCell_position(){
        return this.cell_position;
    }


    public RectangularTextContainer(float top, float left, float width, float height) {
        super(top, left, width, height);
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String s = super.toString();
        sb.append(s.substring(0, s.length() - 1));
        sb.append(String.format(",text=%s]", this.getText() == null ? "null" : "\"" + this.getText() + "\""));
        return sb.toString();
    }
    
    public RectangularTextContainer<T> merge(RectangularTextContainer<T> other) {
        if (this.compareTo(other) < 0) {
            this.getTextElements().addAll(other.getTextElements());
            
        }
        else {
            this.getTextElements().addAll(0, other.getTextElements());
        }
        super.merge(other);
        return this;
    }
    
    public abstract String getText();
    public abstract String getText(boolean useLineReturns);
    public abstract List<T> getTextElements();

    // todo two different getfont method implemented, implement cell one
    public abstract PDFont getFont();
    public abstract float getFontSize();

}
