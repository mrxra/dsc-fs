package li.excessive.fs.wrap;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WrapPath implements Path {
	

	private static Logger log = LoggerFactory.getLogger(WrapPath.class);
	
	protected WrapFilesystem fs = null;
	
	protected Path delegate = null;
	
	// once accessed, keep soft reference to file content serving as cache...
	// memory should be gc'd when required
	protected SoftReference<byte[]> contentCache = null;
	
    public WrapPath (WrapFilesystem fs, Path delegate) {
        this.fs = fs;
        this.delegate = delegate;
    }
    
	protected WrapPath wrap(Path path) {
		WrapPath wrapped = null;
		if(path instanceof WrapPath) {
			wrapped = (WrapPath)path;
		} else {
			try {
				Constructor<? extends WrapPath> c = this.getClass().getDeclaredConstructor(WrapFilesystem.class, Path.class);
				wrapped = c.newInstance(fs, path);
			} catch (Exception e) {
				log.error("failed to wrap {} as {}", path, this.getClass(), e);
			}
		}
		return wrapped;
	}
	
	protected Path unwrap(Path path) {
		return (path instanceof WrapPath) ? ((WrapPath)path).delegate : path;
	}
	
	public Path getDelegate() {
		return this.delegate;
	}
	
	public byte[] getBytes() throws IOException {
		byte[] b = null;
		if(Files.isRegularFile(delegate)) {
			if(null == contentCache || null == contentCache.get()) {
				log.trace("caching file content {}", this);
				contentCache = new SoftReference<byte[]>(Files.readAllBytes(delegate));
			} else {
				log.trace("file content retrieved from cache {}", this);
			}
			b = contentCache.get();
		}
		return b;
	}
	
	@Override
	public WrapFilesystem getFileSystem() {
		return fs;
	}

	@Override
	public boolean isAbsolute() {
		return delegate.isAbsolute();
	}

	@Override
	public Path getRoot() {
		return wrap(delegate.getRoot());
	}

	@Override
	public Path getFileName() {
		return delegate.getFileName();
	}

	@Override
	public Path getParent() {
		return wrap(delegate.getParent());
	}

	@Override
	public int getNameCount() {
		return delegate.getNameCount();
	}

	@Override
	public Path getName(int index) {
		return wrap(delegate.getName(index));
	}

	@Override
	public Path subpath(int beginIndex, int endIndex) {
		return wrap(delegate.subpath(beginIndex, endIndex));
	}

	@Override
	public boolean startsWith(Path other) {
		return delegate.startsWith(other);
	}

	@Override
	public boolean startsWith(String other) {
		return delegate.startsWith(other);
	}

	@Override
	public boolean endsWith(Path other) {
		return delegate.endsWith(other);
	}

	@Override
	public boolean endsWith(String other) {
		return delegate.endsWith(other);
	}

	@Override
	public Path normalize() {
		return wrap(delegate.normalize());
	}

	@Override
	public Path resolve(Path other) {
		return wrap(delegate.resolve(unwrap(other)));
	}

	@Override
	public Path resolve(String other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Path resolveSibling(Path other) {
		return wrap(delegate.resolveSibling(unwrap(other)));
	}

	@Override
	public Path resolveSibling(String other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Path relativize(Path other) {
		return wrap(delegate.relativize(unwrap(other)));
	}

	@Override
	public URI toUri() {
		return fs.provider().wrap(delegate.toUri());
	}

	@Override
	public Path toAbsolutePath() {
		Path p = delegate.toAbsolutePath();
		return wrap(p);
	}

	@Override
	public Path toRealPath(LinkOption... options) throws IOException {
		return wrap(delegate.toRealPath(options));
	}

	@Override
	public File toFile() {
		return delegate.toFile();
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
		return delegate.register(watcher, events, modifiers);
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
		return delegate.register(watcher, events);
	}

	@Override
	public Iterator<Path> iterator() {
		return new Iterator<Path>() {
			Iterator<Path> delegateIt = delegate.iterator();
			@Override
			public boolean hasNext() {
				return delegateIt.hasNext();
			}
			@Override
			public Path next() {
				return wrap(delegateIt.next());
			}
		};
	}

	@Override
	public int compareTo(Path other) {
		return delegate.compareTo(unwrap(other));
	}
	
	@Override
	public String toString() {
		return this.toUri().toString();
	}

}
