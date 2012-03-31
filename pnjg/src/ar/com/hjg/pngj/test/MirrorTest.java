package ar.com.hjg.pngj.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.List;

import ar.com.hjg.pngj.FileHelper;
import ar.com.hjg.pngj.ImageLine;
import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.ChunkCopyBehaviour;
import ar.com.hjg.pngj.chunks.PngChunk;

/**
 * To test all images in PNG test suite (except interlaced) doing a horizontal mirror on all them
 * 
 * Instructions: 
 *  Original images from PNG test suite is supposed to be in local dir
 *    resources/testsuite1/
 *  (images supposed to fail, because are erroneous or because are interlaced, must start with 'x')
 *  Output dir is hardcoded in static "outdir" field - it should be empty
 *  After running main, no error should be thrown 
 *     Errors: 0/141
 *  Result images are mirrored, with a 'z' appended to their names, and the originals are laso copied.  
 *  Suggestion:  sort by name, and watch them in sequence
 *       
 */
public class MirrorTest {
	static final String outdir = "C:/temp/test";
	private static boolean showInfo = false;

	public static void mirror(File orig, File dest) throws Exception {
		PngReader pngr = FileHelper.createPngReader(orig);
		if (showInfo)
			System.out.println(pngr.toString());
		// at this point we have loaded al chucks before IDAT
		// PngWriter pngw = FileHelper.createPngWriter(dest, pngr.imgInfo, true);
		PngWriter pngw = new PngWriter(new FileOutputStream(dest), pngr.imgInfo);
		pngw.setFilterType(FilterType.FILTER_PAETH);
		pngw.copyChunksFirst(pngr, ChunkCopyBehaviour.COPY_ALL_SAFE | ChunkCopyBehaviour.COPY_PALETTE
				| ChunkCopyBehaviour.COPY_TRANSPARENCY);
		ImageLine lout = new ImageLine(pngw.imgInfo);
		int[] line = null;
		int cols = pngr.imgInfo.cols;
		int channels = pngr.imgInfo.channels;
		int aux;
		for (int row = 0; row < pngr.imgInfo.rows; row++) {
			ImageLine l1 = pngr.readRow(row);
			line = l1.tf_unpack(line, false);
			for (int c1 = 0, c2 = cols - 1; c1 < c2; c1++, c2--) {
				for (int i = 0; i < channels; i++) {
					aux = line[c1 * channels + i];
					line[c1 * channels + i] = line[c2 * channels + i];
					line[c2 * channels + i] = aux;
				}
			}
			lout.tf_pack(line, false);
			lout.setRown(l1.getRown());
			pngw.writeRow(lout);
		}
		pngr.end();
		pngw.copyChunksLast(pngr, ChunkCopyBehaviour.COPY_ALL_SAFE | ChunkCopyBehaviour.COPY_TRANSPARENCY);
		pngw.end();
		List<String> u = pngr.getChunksList().getChunksUnkown();
		if(! u.isEmpty()) {
			System.out.println("Unknown chunks:" + u);
		}
	}

	public static void testAllSuite(File dirsrc, File dirdest) {
		if (!dirdest.isDirectory())
			throw new RuntimeException(dirdest + " not a directory");
		int cont = 0;
		int conterr = 0;
		for (File im1 : dirsrc.listFiles()) {
			if (!im1.isFile())
				continue;
			String name = im1.getName();
			if (!name.endsWith(".png"))
				continue;
			File newFile = new File(dirdest, name.replace(".png", "z.png"));
			File fileCopy = new File(dirdest, name);
			try {
				cont++;
				mirror(im1, newFile);
				if (name.startsWith("x")) {
					System.err.println("this should have failed! " + name);
					conterr++;
				}
			} catch (Exception e) {
				if (name.startsWith("x")) { // suppposed to fail
					System.out.println("ok error with " + name + " " + e.getMessage());
				} else { // real error
					System.err.println("error with " + name + " " + e.getMessage());
					conterr++;
					throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
				}
			} finally {
				if (name.startsWith("x")) { // suppposed to fail: remove it
					try {
						newFile.delete();
					} catch (Exception e) {
					}
				} else {
					copyFile(im1, fileCopy);
				}
			}
		}
		System.out.println("Errors: " + conterr + "/" + cont);
	}

	private static void copyFile(File sourceFile, File destFile) {
		try {
			if (!destFile.exists()) {
				destFile.createNewFile();
			}
			FileChannel source = null;
			FileChannel destination = null;
			try {
				source = new FileInputStream(sourceFile).getChannel();
				destination = new FileOutputStream(destFile).getChannel();
				destination.transferFrom(source, 0, source.size());
			} finally {
				if (source != null) {
					source.close();
				}
				if (destination != null) {
					destination.close();
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void test1() throws Exception {
		// reencode("resources/testsuite1/basn0g01.png", "C:/temp/x.png");
		// reencode(new File("resources/testsuite1/basn0g02.png"), new
		// File("C:/temp/x2.png"));
		mirror(new File("resources/testsuite1/basn3p08.png"), new File("C:/temp/test/xxx.png"));
		System.out.println("done: ");
	}

	
	public static void main(String[] args) throws Exception {
		testAllSuite(new File("resources/testsuite1/"), new File(outdir));
		
		System.out.println("output dir: " + outdir);

	}
}
