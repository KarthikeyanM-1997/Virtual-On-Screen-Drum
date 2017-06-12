
//Swing Imports
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

//File Handling Imports
import java.io.File;
import java.io.IOException;

//Midi Imports
import javax.imageio.ImageIO;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

//OpenCV Imports
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;

public class JPanelOpenCV extends JFrame {

	static JLabel jl;

	static double rX, rY, cX, cY;

	public static void main(String args[]) throws InterruptedException, MidiUnavailableException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		JFrame fr = new JFrame();
		jl = new JLabel();

		VideoCapture camera = new VideoCapture(0);

		Mat frame = new Mat();
		camera.open(0);
		camera.read(frame);

		System.out.println(frame.width() + " " + frame.height());

		fr.setSize(frame.width() + 100, frame.height() + 100);
		fr.setVisible(true);
		fr.setLayout(new FlowLayout());
		fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		fr.add(jl);

		BufferedImage img = null;
		try {
			img = ImageIO.read(new File("drums.png"));
		} catch (IOException e) {
			System.out.println("Overlay image not found !");
		}

		Mat imgThresholded = new Mat();
		
		Synthesizer midiSynth = MidiSystem.getSynthesizer();
		midiSynth.open();
		Instrument[] instr = midiSynth.getDefaultSoundbank().getInstruments();
		MidiChannel[] mChannels = midiSynth.getChannels();
		midiSynth.loadInstrument(instr[114]);
		
		while (true) {
			Mat hsv = new Mat();
			Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_RGB2HSV);
			Core.flip(hsv, hsv, 1);
			Core.inRange(hsv, new Scalar(100, 100, 100, 0), new Scalar(112, 255, 255, 0), imgThresholded);

			Imgproc.erode(imgThresholded, imgThresholded,
					Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(10, 10)));
			Imgproc.dilate(imgThresholded, imgThresholded,
					Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(10, 10)));
			Imgproc.dilate(imgThresholded, imgThresholded,
					Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(10, 10)));
			Imgproc.erode(imgThresholded, imgThresholded,
					Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(10, 10)));

			BufferedImage drumsOverlay = new BufferedImage(frame.width(), frame.height(), BufferedImage.TYPE_INT_ARGB);

			// paint both images
			Graphics g = drumsOverlay.getGraphics();
			g.drawImage(matToBufferedImage(frame), 0, 0, null);
			g.drawImage(img, 0, 0, null);

			updateImage(drumsOverlay);
			
			rX = cX;
			rY = cY;

			Moments oMoments = Imgproc.moments(imgThresholded);
			double dM01 = oMoments.m01;
			double dM10 = oMoments.m10;
			double dArea = oMoments.m00;
			
			//System.out.println(dM01 + " " + dM10 + " " + dArea);

			double velocity = 0;
			if (dArea > 10000) {
				// calculate the position of the ball
				double posX = dM10 / dArea;
				double posY = dM01 / dArea;
				cX = posX;
				cY = posY;
				velocity = Math.sqrt(Math.pow(cX - rX, 2) + Math.pow(cY - rY, 2));
			}
			
			
			try {
				camera.read(frame);
				// Thread.sleep(5);
				if (velocity > 1 && cY > 425) {
					mChannels[1].noteOn(50, (int) velocity * 2);
				}
			} catch (Exception e) {

			}

		}
	}

	// Method to update image on frame
	static void updateImage(BufferedImage img) {
		jl.setIcon(new ImageIcon(img));
	}

	// Utility functions

	// Load an image
	public static BufferedImage loadImage(String file) {
		BufferedImage img;

		try {
			File input = new File(file);
			img = ImageIO.read(input);
			return img;
		} catch (Exception e) {
			System.out.println("error");
		}

		return null;
	}

	// Save an image
	public void saveImage(BufferedImage img) {
		try {
			File outputfile = new File("Images/new.png");
			ImageIO.write(img, "png", outputfile);
		} catch (Exception e) {
			System.out.println("error");
		}
	}

	public static BufferedImage matToBufferedImage(Mat matrix) {
		int cols = matrix.cols();
		int rows = matrix.rows();
		int elemSize = (int) matrix.elemSize();
		byte[] data = new byte[cols * rows * elemSize];
		int type;
		matrix.get(0, 0, data);
		switch (matrix.channels()) {
		case 1:
			type = BufferedImage.TYPE_BYTE_GRAY;
			break;
		case 3:
			type = BufferedImage.TYPE_3BYTE_BGR;
			// bgr to rgb
			byte b;
			for (int i = 0; i < data.length; i = i + 3) {
				b = data[i];
				data[i] = data[i + 2];
				data[i + 2] = b;
			}
			break;
		default:
			return null;
		}
		BufferedImage image2 = new BufferedImage(cols, rows, type);
		image2.getRaster().setDataElements(0, 0, cols, rows, data);
		return image2;
	}

	public static Mat bufferedImageToMat(BufferedImage bi) {
		Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
		byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
		mat.put(0, 0, data);
		return mat;
	}

}