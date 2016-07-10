package li.excessive.fs.wrap;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WrapFileStore extends FileStore {

	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(WrapFileStore.class);
	
	protected FileStore delegate = null;
	
	public WrapFileStore(FileStore delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public String name() {
		return delegate.name();
	}

	@Override
	public String type() {
		return "wrap";
	}

	@Override
	public boolean isReadOnly() {
		return delegate.isReadOnly();
	}

	@Override
	public long getTotalSpace() throws IOException {
		return delegate.getTotalSpace();
	}

	@Override
	public long getUsableSpace() throws IOException {
		return delegate.getUsableSpace();
	}

	@Override
	public long getUnallocatedSpace() throws IOException {
		return delegate.getUnallocatedSpace();
	}

	@Override
	public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
		return delegate.supportsFileAttributeView(type);
	}

	@Override
	public boolean supportsFileAttributeView(String name) {
		return delegate.supportsFileAttributeView(name);
	}

	@Override
	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
		return delegate.getFileStoreAttributeView(type);
	}

	@Override
	public Object getAttribute(String attribute) throws IOException {
		return delegate.getAttribute(attribute);
	}

}
