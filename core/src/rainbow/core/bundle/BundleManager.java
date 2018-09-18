package rainbow.core.bundle;

import java.util.Collection;
import java.util.List;

import com.google.common.base.Predicate;

/**
 * bundle管理接口
 * 
 * @author lijinghui
 * 
 */
public interface BundleManager {

    /**
     * 返回Bundle列表
     * 
     * @param filter
     *            过滤条件
     * @return
     */
    public Collection<Bundle> getBundles(Predicate<Bundle> filter);

    /**
     * 返回指定Bundle
     * 
     * @param id
     * @return
     */
    public Bundle get(String id);

    /**
     * 刷新Bundle及状态列表
     */
    public void refresh();

    /**
     * 初始化启动的bundle
     * 
     * @param onList
     *            初始化要启动的bundle列表
     * @param offList
     *            初始化不要启动的bundle列表
     */
    public void initStart(List<String> onList, List<String> offList);

    /**
     * 停止所有的bundle
     */
    public void stopAll();

    /**
     * 启动指定Bundle
     * 
     * @param id
     * @return
     * @throws BundleException
     */
    public boolean startBundle(String id) throws BundleException;

    /**
     * 停止指定Bundle
     * 
     * @param id
     */
    public void stopBundle(String id) throws BundleException;

    /**
     * 卸载指定bundle
     * 
     * @param id
     */
    public void uninstallBundle(String id);

}
