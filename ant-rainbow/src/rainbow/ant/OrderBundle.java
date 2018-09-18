package rainbow.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import rainbow.core.bundle.BundleData;

/**
 * Bundle 排序任务，指定bundle目录，根据bundle依赖排序，将结果用，分割放在属性指定的属性中
 * 
 * @author lijinghui
 * 
 */
public class OrderBundle extends Task {

    private File dir;

    private String property;

    public void setDir(File dir) {
        this.dir = dir;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    @Override
    public void execute() throws BuildException {
        if (property == null) {
            throw new BuildException("No property");
        }
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            throw new BuildException("bad dir attribute");
        }
        Map<String, BundleData> map = new HashMap<String, BundleData>();
        try {
            foundBundle(map);
        } catch (JAXBException e) {
            new BuildException(e);
        }
        List<String> list = new ArrayList<String>(map.size());
        System.out.println(String.format("found %d bundles", map.size()));
        int left = 0;
        while (!map.isEmpty()) {
            if (map.size() == left) {
                for (String id : map.keySet()) {
                    System.out.println("please check: " + id);
                }
                throw new BuildException("cycle dependent found!");
            }
            left = map.size();
            System.out.println(String.format("new round, %d left", left));
            String[] ids = map.keySet().toArray(new String[map.size()]);
            for (String id : ids) {
                BundleData data = map.get(id);
                String parent[] = data.getParents();
                if (parent == null) {
                    list.add(id);
                    map.remove(id);
                } else {
                    int count = parent.length;
                    for (int i = 0; i < parent.length; i++) {
                        if (map.containsKey(parent[i]))
                            break;
                        else
                            count--;
                    }
                    if (count == 0) {
                        list.add(id);
                        map.remove(id);
                    }
                }
            }
        }
        if (!list.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String str : list) {
                if (sb.length() > 0)
                    sb.append(",");
                sb.append(str);
            }
            getProject().setNewProperty(property, sb.toString());
        }
    }

    private void foundBundle(Map<String, BundleData> map) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(BundleData.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                File bundleFile = new File(file, "/src/bundle.xml");
                if (bundleFile.exists()) {
                    try {
                        BundleData data = (BundleData) unmarshaller.unmarshal(bundleFile);
                        if (file.getName().equals(data.getId())) {
                            map.put(data.getId(), data);
                        } else
                            System.out.println(String.format("invalid bundle id [%s] of bundle [%s]", data.getId(),
                                    file.getName()));
                    } catch (JAXBException e) {
                        System.out.println(String.format("invalid bundle.xml of [%s]", file.getName()));
                    }
                }
            }
        }
    }
}
