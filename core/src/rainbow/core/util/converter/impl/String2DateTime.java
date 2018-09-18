package rainbow.core.util.converter.impl;

import java.sql.Timestamp;

import org.joda.time.DateTime;

import rainbow.core.util.converter.AbstractConverter;

/**
 * 字符串转为DateTime，格式为 yyyy-mm-dd hh:mm:ss[.fffffffff] 或 yyyy-mm-ddThh:mm:ss[.fffffffff]
 * 
 * @author lijinghui
 *
 */
public class String2DateTime extends AbstractConverter<String, DateTime> {

	@Override
	public DateTime convert(String from, Class<?> toClass) {
		try {
			return new DateTime(from);
		} catch (IllegalArgumentException e) {
			return new DateTime(Timestamp.valueOf(from));
		}
	}

}
