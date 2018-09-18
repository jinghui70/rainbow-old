package rainbow.core.util.converter.impl;

import org.joda.time.LocalDate;

import rainbow.core.util.converter.AbstractConverter;

/**
 * 字符串转为LocalDate，字符串默认为yyyy-MM-dd格式
 * 
 * @author lijinghui
 *
 */
public class LocalDate2SqlDate extends AbstractConverter<LocalDate, java.sql.Date> {

	@Override
	public java.sql.Date convert(LocalDate from, Class<?> toClass) {
		return new java.sql.Date(from.toDate().getTime());
	}

}
