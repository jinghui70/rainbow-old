package rainbow.ant.rdm;

import java.util.LinkedList;
import java.util.List;

public class Template {

	/**
	 * 模版名
	 */
	private String name;

	/**
	 * 转换后的文件后缀名
	 */
	private String suffix;

	private List<Param> params = new LinkedList<Param>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSuffix() {
		if (suffix == null)
			return "sql";
		else
			return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public List<Param> getParams() {
		return params;
	}

	public void setParams(List<Param> params) {
		this.params = params;
	}

	public void addParam(Param param) {
		params.add(param);
	}
}
