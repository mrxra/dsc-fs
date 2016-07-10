package li.excessive.fs.meta;

import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import li.excessive.fs.wrap.WrapFileStore;

public class MetaDataFileStore extends WrapFileStore {
	
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(MetaDataFileStore.class);
	
	public MetaDataFileStore(FileStore delegate) {
		super(delegate);
	}
	
	@Override
	public String type() {
		return "meta";
	}

	@Override
	public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
		return delegate.supportsFileAttributeView(type)
				|| MetaDataView.class == type;
	}

	@Override
	public boolean supportsFileAttributeView(String name) {
		return delegate.supportsFileAttributeView(name) 
				|| MetaDataView.NAME.equals(name);
	}

}
