package rdm.editors;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import rainbow.core.util.XmlBinder;
import rainbow.db.model.Model;

public abstract class TransformUtility {
	private static XmlBinder<Model> binder = Model.getXmlBinder();
	private static TransformerFactory tf = TransformerFactory.newInstance();

	public static Transformer getTransformer(File file) throws TransformerConfigurationException {
		return tf.newTransformer(new StreamSource(file));
	}

	public static Model readModel(InputStream is) throws JAXBException {
		return binder.unmarshal(is);
	}

	public static void transform(Model model, OutputStream os) {
		try {
			binder.marshal(model, os);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
