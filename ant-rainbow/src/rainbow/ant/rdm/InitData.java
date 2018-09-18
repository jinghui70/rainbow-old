package rainbow.ant.rdm;

/**
 * 初始化数据描述。初始化数据可能是一个sql，也可能是一个载有数据的excel文件。会生成一个sql
 * 
 * @author lijinghui
 *
 */
public class InitData {

	/**
	 * 生成的sql文件名
	 */
	private String name;
	
	/**
	 * 初始化数据所在目录
	 */
	private String dir;
	
	/**
	 * 如果有H2模版，则会生成h2数据库，这个标志为true将把初始化数据导入到刚生成的数据库中
	 */
	private boolean load;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}

	public boolean isLoad() {
		return load;
	}

	public void setLoad(boolean load) {
		this.load = load;
	}

}
