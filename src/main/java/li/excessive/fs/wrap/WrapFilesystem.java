package li.excessive.fs.wrap;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WrapFilesystem extends FileSystem {

	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(WrapFilesystem.class);
	
	protected WrapFilesystemProvider provider = null;
	
	protected FileSystem delegate = null;
	
	public static final String CLOSE_CASCADE = "wrap.close.cascade";
	private Boolean closeCascade = false;
	
	public WrapFilesystem(WrapFilesystemProvider provider, FileSystem delegate, Map<String, ?> env){
		this.provider = provider;
		this.delegate = delegate;
		closeCascade = env.containsKey(CLOSE_CASCADE) ? (Boolean)env.get(CLOSE_CASCADE) : Boolean.FALSE;
	}

	@Override
	public WrapFilesystemProvider provider() {
		return provider;
	}
	
	@Override
	public Path getPath(String first, String... more) {
		return new WrapPath(this, delegate.getPath(first, more));
	}
	

	@Override
	public void close() throws IOException {
		if(closeCascade) {
			delegate.close();
		}
		this.provider().unregister(this);
	}

	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public boolean isReadOnly() {
		return delegate.isReadOnly();
	}

	@Override
	public String getSeparator() {
		return delegate.getSeparator();
	}

	@Override
	public Iterable<Path> getRootDirectories() {
		Iterable<Path> itr = StreamSupport.stream(delegate.getRootDirectories().spliterator(), false)
				.map(it -> new WrapPath(this, it)).collect(Collectors.toList());
		return itr;
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		Iterable<FileStore> itr = StreamSupport.stream(delegate.getFileStores().spliterator(), false)
				.map(it -> this.provider().wrap(it)).collect(Collectors.toList());
		return itr;
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		return delegate.supportedFileAttributeViews();
	}

	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern) {
		throw new UnsupportedOperationException();
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
	}

	@Override
	public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
	}

}
