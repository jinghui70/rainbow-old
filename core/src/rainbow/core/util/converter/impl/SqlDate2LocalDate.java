package rainbow.core.util.converter.impl;

import org.joda.time.LocalDate;

import rainbow.core.util.converter.AbstractConverter;

/**
 * 
 * @author lijinghui
 *
 */
public class SqlDate2LocalDate extends AbstractConverter<java.sql.Date, LocalDate> {

	@Override
	public LocalDate convert(java.sql.Date from, Class<?> toClass) {
		return new LocalDate(from);
	}

}
