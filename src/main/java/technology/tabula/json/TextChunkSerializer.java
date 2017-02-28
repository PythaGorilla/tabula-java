package technology.tabula.json;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import com.google.gson.*;
import org.apache.pdfbox.pdmodel.font.PDFont;
import technology.tabula.RectangularTextContainer;

import technology.tabula.TextElement;

public class TextChunkSerializer implements JsonSerializer<RectangularTextContainer> {
    //todo cell serializer
    @Override
    public JsonElement serialize(RectangularTextContainer textChunk, Type arg1,
            JsonSerializationContext context) {
        JsonObject object = new JsonObject();

        object.addProperty("top", textChunk.getTop());
        object.addProperty("left", textChunk.getLeft());
        object.addProperty("width", textChunk.getWidth());
        object.addProperty("height", textChunk.getHeight());
        object.addProperty("text", textChunk.getText());
        object.addProperty("row", textChunk.getCell_position().row);
        object.addProperty("col", textChunk.getCell_position().col);
        List<TextElement> textElements=textChunk.getTextElements();
        //todo cell json serialization
//        JsonArray jsonDataArray = new JsonArray();
//        for (TextElement textElement: textElements) {
//
//            jsonDataArray.add(textElement.toString());
//        }
//        object.add("data", jsonDataArray);


        if (textChunk.getFont() !=null){
            PDFont cell_font =  textChunk.getFont();

            if (cell_font.getBaseFont() !=null) {
                String base_font = cell_font.getBaseFont();
                object.addProperty("baseFont", String.valueOf(base_font));
                object.addProperty("fontSize", textChunk.getFontSize());

                if (cell_font.getFontDescriptor() !=null) {
                    float font_weight = cell_font.getFontDescriptor().getFontWeight();
                //todo modify cell output
                object.addProperty("fontWeight", String.valueOf(font_weight));
            }}}
        else{
            object.addProperty("baseFont", "");
            object.addProperty("fontSize", "0");
            object.addProperty("fontWeight", "0");
        }

        return object;
    }

}