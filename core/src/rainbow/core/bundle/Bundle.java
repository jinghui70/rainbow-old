package rainbow.core.bundle;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;

public class Bundle {

    /**
     * Bundle的配置信息
     */
    private BundleData data;

    private BundleState state = BundleState.FOUND;

    private BundleClassLoader classLoader;

    /**
     * 所有的前辈
     */
    private ImmutableList<Bundle> ancestors = ImmutableList.of();

    /**
     * 父辈
     */
    private ImmutableList<Bundle> parents = ImmutableList.of();

    /**
     * Bundle入口类
     */
    BundleActivator activator;

    public Bundle(BundleData data, BundleClassLoader classLoader) {
        this.data = data;
        this.classLoader = classLoader;
        this.classLoader.setBundle(this);
    }

    public BundleClassLoader getClassLoader() {
        return classLoader;
    }

    public BundleData getData() {
        return data;
    }

    public String getId() {
        return data.getId();
    }

    public String getDesc() {
        return data.getDesc();
    }

    public String[] getParentId() {
        return data.getParents();
    }

    public BundleState getState() {
        return state;
    }

    public void setState(BundleState state) {
        this.state = state;
    }

    public ImmutableList<Bundle> getAncestors() {
        if (ancestors == null)
            return ImmutableList.of();
        return ancestors;
    }

    void setAncestors(ImmutableList<Bundle> ancestors) {
        this.ancestors = ancestors;
    }

    public ImmutableList<Bundle> getParents() {
        return parents;
    }

    void setParents(ImmutableList<Bundle> parents) {
        if (parents == null)
            this.parents = ImmutableList.of();
        else
            this.parents = parents;
    }

    void setActivator(BundleActivator activator) {
        this.activator = activator;
    }

    BundleActivator createActivator() throws BundleException {
        try {
            Class<?> activatorClass = null;
			String className = getId() + ".Activator";
            try {
                activatorClass = classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                className = String.format("rainbow.%s.Activator", getId());
                activatorClass = classLoader.loadClass(className);
            }
            checkState(BundleActivator.class.isAssignableFrom(activatorClass), "wrong activator class %s",
                activatorClass);
            BundleActivator activator = (BundleActivator) activatorClass.newInstance();
            activator.setBundleId(getId());
            return activator;
        } catch (Throwable e) {
            throw new BundleException("init activator failed", e);
        }
    }

    public BundleActivator getActivator() {
        return activator;
    }

    public void destroy() {
        setParents(null);
        setAncestors(null);
        classLoader.destroy();
        classLoader = null;
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public int hashCode() {
        return data.getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Bundle other = (Bundle) obj;
        return getId().equals(other.getId());
    }
}
