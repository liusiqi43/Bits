package utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.siqi.bits.Task;

import java.lang.reflect.Type;

/**
 * Created by me on 7/27/14.
 */
public class BitsJsonSerializer implements JsonSerializer<Task> {
    @Override
    public JsonElement serialize(Task task, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("done_on", task.getDoneOn().getTime());
        jsonObject.addProperty("desc", task.getDescription());

        return jsonObject;
    }
}
