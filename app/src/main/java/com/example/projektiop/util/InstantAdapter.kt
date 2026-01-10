package com.example.projektiop.util

import com.google.gson.*
import java.time.Instant
import java.lang.reflect.Type

class InstantAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Instant {
        return Instant.parse(json.asString)
    }

    override fun serialize(src: Instant, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toString())
    }
}
