package com.sapuseven.noticap.utils;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

class Compressor {
	private static byte[] compress(byte[] data) throws IOException {
		Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
		deflater.setInput(data);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
		deflater.finish();
		byte[] buffer = new byte[1024];
		while (!deflater.finished()) {
			int count = deflater.deflate(buffer);
			outputStream.write(buffer, 0, count);
		}
		outputStream.close();
		byte[] output = outputStream.toByteArray();
		Log.d("Compressor", "Original: " + data.length);
		Log.d("Compressor", "Compressed: " + output.length);
		return output;
	}

	static byte[] decompress(byte[] data) throws IOException, DataFormatException {
		Inflater inflater = new Inflater(true);
		inflater.setInput(data);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
		byte[] buffer = new byte[1024];
		while (!inflater.finished()) {
			int count = inflater.inflate(buffer);
			outputStream.write(buffer, 0, count);
		}
		outputStream.close();
		byte[] output = outputStream.toByteArray();
		Log.d("Compressor", "Original: " + data.length);
		Log.d("Compressor", "Decompressed: " + output.length);
		return output;
	}

	static byte[] readFile(File file) throws IOException {
		byte[] content = new byte[(int) file.length()];
		FileInputStream fis = new FileInputStream(file);
		//noinspection ResultOfMethodCallIgnored
		fis.read(content);
		fis.close();
		return content;
	}

	public static byte[] readStream(InputStream inputStream) throws IOException {
		// this dynamically extends to take the bytes you read
		ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

		// this is storage overwritten on each iteration with bytes
		int bufferSize = 1024;
		byte[] buffer = new byte[bufferSize];

		// we need to know how may bytes were read to write them to the byteBuffer
		int len = 0;
		while ((len = inputStream.read(buffer)) != -1) {
			byteBuffer.write(buffer, 0, len);
		}

		// and then we can return your byte array.
		return byteBuffer.toByteArray();
	}

	static void writeFile(File file, byte[] data) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		fos.write(Compressor.compress(data));
		fos.close();
	}
}
