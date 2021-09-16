package jadx.plugins.input.java.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisasmUtils {
	private static final Logger LOG = LoggerFactory.getLogger(DisasmUtils.class);

	public static String get(byte[] bytes) {
		return useASM(bytes);
	}

	private static String useASM(byte[] bytes) {
		StringWriter out = new StringWriter();
		TraceClassVisitor tcv = new TraceClassVisitor(new PrintWriter(out));
		new ClassReader(bytes).accept(tcv, 0);
		return out.toString();
	}

	/**
	 * Use javap as a temporary disassembler for java bytecode
	 * Don't remove! Useful for debug.
	 */
	private static String useSystemJavaP(byte[] bytes) {
		try {
			Path tmpCls = null;
			try {
				tmpCls = Files.createTempFile("jadx", ".class");
				Files.write(tmpCls, bytes, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
				Process process = Runtime.getRuntime().exec(new String[] {
						"javap", "-constants", "-v", "-p", "-c",
						tmpCls.toAbsolutePath().toString()
				});
				process.waitFor(2, TimeUnit.SECONDS);
				return inputStreamToString(process.getInputStream());
			} finally {
				if (tmpCls != null) {
					Files.delete(tmpCls);
				}
			}
		} catch (Exception e) {
			LOG.error("Java class disasm error", e);
			return "error";
		}
	}

	public static String inputStreamToString(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[8 * 1024];
		while (true) {
			int r = in.read(buf);
			if (r == -1) {
				break;
			}
			out.write(buf, 0, r);
		}
		return out.toString();
	}
}
