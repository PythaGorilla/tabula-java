package technology.tabula.json;

import java.lang.reflect.Type;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.font.PDFont;
import technology.tabula.RectangularTextContainer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

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
        object.addProperty("position", textChunk.getCell_position().toString());

        if (textChunk.getFont() !=null){
            PDFont cell_font =  textChunk.getFont();


            if (cell_font.getFontDescriptor() !=null) {
                String base_font = cell_font.getFontDescriptor().getFontName();
                object.addProperty("baseFont", String.valueOf(base_font));
                float font_weight = textChunk.getFont().getFontDescriptor().getFontWeight();
                //todo modify cell output
                object.addProperty("baseFont", String.valueOf(base_font));
                object.addProperty("fontWeight", String.valueOf(font_weight));
            }
    }
        return object;
    }

}