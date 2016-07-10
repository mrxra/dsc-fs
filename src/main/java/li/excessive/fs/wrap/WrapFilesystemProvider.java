package li.excessive.fs.wrap;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * wrapping file system provider.
 * provides means to wrap and delegate to existing file systems
 */
public class WrapFilesystemProvider extends FileSystemProvider {

	private static Logger log = LoggerFactory.getLogger(WrapFilesystemProvider.class);
	
    private final Map<URI, WrapFilesystem> filesystems = new HashMap<>();
    
	protected URI wrap(URI uri) {
		return this.getScheme().equals(uri.getScheme()) ? uri : URI.create(this.getScheme() + ":" + uri.toString());
	}
	protected URI unwrap(URI uri) {
		if(this.getScheme().equals(uri.getScheme())) {
			uri = URI.create(uri.getSchemeSpecificPart());
			if(null == uri.getScheme()) {
				uri = URI.create(FileSystems.getDefault().provider().getScheme() + ":" + uri.toString());
			}
		}
		return uri;
	}
	
	protected WrapPath wrap(Path path) {
		return (path instanceof WrapPath) ? (WrapPath)path : new WrapPath(this.register(path.getFileSystem()), path);
	}
	
	protected Path unwrap(Path path) {
		return (path instanceof WrapPath) ? ((WrapPath)path).getDelegate() : path;
	}
	
	protected WrapFileStore wrap(FileStore filestore) {
		return new WrapFileStore(filestore);
	}
	
	protected WrapFilesystem wrap(FileSystem delegate, Map<String, ?> env) {
		return new WrapFilesystem(this, delegate, env);
	}
		
	@Override
	public String getScheme() {
		return "wrap";
	}
	
	
	/**
	 * create a filesystem or throw exception if it already exists. the given uri has to point
	 * to the root directory of the filesystem to be created.
	 * @param rootWrapUri
	 * @param env
	 * @return
	 * @throws IOException
	 */
	@Override
	public WrapFilesystem newFileSystem(URI rootWrapUri, Map<String, ?> env) throws IOException {
        WrapFilesystem fs = null;
        synchronized(filesystems) {
        	URI rootUri = this.unwrap(rootWrapUri);
        	
        	// return existing fs (unless closed)...
        	if(filesystems.containsKey(rootUri) && filesystems.get(rootUri).isOpen()) {
        		fs = filesystems.get(rootUri);
        	} else {
	        	FileSystem delegate = null;
	        	Map<String, Object> args = new HashMap<>();
	        	try {
	        		delegate = FileSystems.getFileSystem(rootUri);
	        		args.put(WrapFilesystem.CLOSE_CASCADE, Boolean.FALSE);
	        	} catch (FileSystemNotFoundException e) {
	        		delegate = FileSystems.newFileSystem(rootUri, env);
	        		args.put(WrapFilesystem.CLOSE_CASCADE, Boolean.TRUE);
	        	}
	            fs = this.wrap(delegate, args);
	            filesystems.put(rootUri, fs);
        	}
        }
		return fs;
	}
	
	/**
	 * for a given path delegate, try to find the corresponding wrap file system. 
	 * if not found, wrap and register the path's file system.
	 * @param path
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <T extends WrapFilesystem> T register(FileSystem delegate) {
		WrapFilesystem fs = null;
		synchronized(filesystems) {
			Set<WrapFilesystem> l = StreamSupport.stream(delegate.getRootDirectories().spliterator(), false)
				.map(it -> filesystems.get(it.toUri()))
				.filter(it -> null != it)
				.collect(Collectors.toSet());
			if(l.isEmpty()) {
	        	Map<String, Object> args = new HashMap<>();
        		args.put(WrapFilesystem.CLOSE_CASCADE, Boolean.FALSE);
	            fs = this.wrap(delegate, args);
	            for(Path root : delegate.getRootDirectories()) {
		            log.debug("registering filesystem " + root.toUri());
		            filesystems.put(root.toUri(), fs);
	            }
			} else {
				fs = l.iterator().next();
			}
		}
		return (T)fs;
	}
	
	protected void unregister(WrapFilesystem fs) {
		synchronized(filesystems) {
            for(Path root : fs.delegate.getRootDirectories()) {
	            log.debug("unregistering filesystem " + root.toUri());
	            filesystems.remove(root.toUri());
            }
		}
	}

	@Override
	public WrapFilesystem getFileSystem(URI rootWrapUri) {
		return register(Paths.get(this.unwrap(rootWrapUri)).getFileSystem());
	}
	
	/**
	 * factory method used by {@link Paths#get(URI)} facade
	 */
	@Override
	public Path getPath(URI wrapUri) {
		return this.wrap(Paths.get(this.unwrap(wrapUri)));
	}
	
	
	@Override
	public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
		return Files.newByteChannel(this.unwrap(path), options);
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
		if(! (dir instanceof WrapPath))
			throw new IllegalArgumentException("invalid path type " + dir.getClass());
		
		return new DirectoryStream<Path>() {
			DirectoryStream<Path> delegate = Files.newDirectoryStream(((WrapPath)dir).getDelegate());
			@Override
			public void close() throws IOException { delegate.close(); }
			
			@Override
			public Iterator<Path> iterator() {
				return new Iterator<Path>() {
					Iterator<Path> it = delegate.iterator();
					@Override
					public boolean hasNext() { return it.hasNext(); }
					@Override
					public WrapPath next() { return wrap(it.next()); }
				};
			}
		};
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
		Path p = this.unwrap(dir);
		Files.createDirectory(p, attrs);
	}

	@Override
	public void delete(Path path) throws IOException {
		Files.delete(this.unwrap(path));
	}

	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException {
		Files.copy(this.unwrap(source), this.unwrap(target), options);
	}

	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException {
		Files.move(this.unwrap(source), this.unwrap(target), options);
	}

	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isHidden(Path path) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException {
		return this.wrap(Files.getFileStore(this.unwrap(path)));
	}

	// e.g. called by Files.exists(...)
	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException {
		Path p = this.unwrap(path);
		p.getFileSystem().provider().checkAccess(p, modes);
	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
		return Files.getFileAttributeView(this.unwrap(path), type, options);
	}

	@Override
	public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
		return Files.readAttributes(this.unwrap(path), type, options);
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
		throw new UnsupportedOperationException();
	}

}
