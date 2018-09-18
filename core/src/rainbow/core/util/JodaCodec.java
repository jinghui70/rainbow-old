package rainbow.core.util;

import java.io.IOException;
import java.lang.reflect.Type;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;

public class JodaCodec implements ObjectSerializer, ObjectDeserializer {

	public static final JodaCodec instance = new JodaCodec();

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
		JSONLexer lexer = parser.getLexer();
		if (lexer.token() == JSONToken.NULL) {
			lexer.nextToken();
			return null;
		}
		if (lexer.token() == JSONToken.LITERAL_STRING) {
			String text = lexer.stringVal();
			lexer.nextToken();
			if ("".equals(text))
				return null;
			if (type == LocalDateTime.class) {
				return (T) LocalDateTime.parse(text);
			} else if (type == LocalDate.class) {
				return (T) LocalDateTime.parse(text).toLocalDate();
			} else if (type == LocalTime.class) {
				return (T) LocalTime.parse(text);
			} else if (type == DateTime.class) {
				return (T) DateTime.parse(text);
			} else if (type == Period.class) {
				return (T) Period.parse(text);
			} else if (type == Duration.class) {
				return (T) Duration.parse(text);
			} else if (type == Instant.class) {
				return (T) Instant.parse(text);
			}
		} else {
			throw new UnsupportedOperationException();
		}
		return null;
	}

	@Override
	public int getFastMatchToken() {
		return JSONToken.LITERAL_STRING;
	}

	@Override
	public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features)
			throws IOException {
		SerializeWriter out = serializer.getWriter();

		if (object == null) {
			out.writeNull();
			return;
		}

		out.writeString(object.toString());
	}
}