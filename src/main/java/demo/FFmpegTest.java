package demo;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bytedeco.javacpp.*;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;

//ffmpeg
import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;
import static org.bytedeco.javacpp.swscale.*;
//opencv
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_highgui.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_stitching.*;

public class FFmpegTest {

    public static void main(String[] args) throws IOException {
	AVFormatContext pFormatCtx = new AVFormatContext(null);
	int i, videoStream;
	AVCodecContext pCodecCtx = null;
	AVCodec pCodec = null;
	AVFrame pFrame = null;
	AVFrame pFrameRGB = null;
	AVPacket packet = new AVPacket();
	int[] frameFinished = new int[1];
	int numBytes;
	BytePointer buffer = null;

	AVDictionary optionsDict = null;
	SwsContext sws_ctx = null;
	av_register_all();
	avformat_network_init();
// Register all formats and codecs
//  String in="C:/img/video/m.h264";
	String in = "rtsp://quicktime.tc.columbia.edu:554/users/lrf10/movies/sixties.mov";
//  String in="rtmp://192.168.10.192/oflaDemo/stream live=1";
//  String in="rtsp://123.124.159.5:554/pag://192.168.1.5:7302:81ef249ced3a454aaadda66a92697bf2:1:MAIN:TCP?cnid=4&pnid=5";
// Open video file
	if (avformat_open_input(pFormatCtx, in, null, null) != 0) {
	    System.exit(-1); // Couldn't open file
	}

// Retrieve stream information
	if (avformat_find_stream_info(pFormatCtx, (PointerPointer) null) < 0) {
	    System.exit(-1); // Couldn't find stream information
	}

// Dump information about file onto standard error
	av_dump_format(pFormatCtx, 0, in, 0);

// Find the first video stream
	videoStream = -1;
	for (i = 0; i < pFormatCtx.nb_streams(); i++) {
	    if (pFormatCtx.streams(i).codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) {
		videoStream = i;
		break;
	    }
	}
	if (videoStream == -1) {
	    System.exit(-1); // Didn't find a video stream
	}

// Get a pointer to the codec context for the video stream
	pCodecCtx = avcodec_alloc_context3(null);
	avcodec_parameters_to_context(pCodecCtx, pFormatCtx.streams(videoStream).codecpar());
//  pCodecCtx = pFormatCtx.streams(videoStream).codec();

// Find the decoder for the video stream
	pCodec = avcodec_find_decoder(pCodecCtx.codec_id());
	if (pCodec == null) {
	    System.err.println("Unsupported codec!");
	    System.exit(-1); // Codec not found
	}
// Open codec
	if (avcodec_open2(pCodecCtx, pCodec, optionsDict) < 0) {
	    System.exit(-1); // Could not open codec
	}

// Allocate video frame
	pFrame = av_frame_alloc();

// Allocate an AVFrame structure
	pFrameRGB = av_frame_alloc();
	if (pFrameRGB == null) {
	    System.exit(-1);
	}

// Determine required buffer size and allocate buffer
//        numBytes = avpicture_get_size(AV_PIX_FMT_RGB24,pCodecCtx.width(),pCodecCtx.height());
	numBytes = av_image_get_buffer_size(AV_PIX_FMT_BGR24, 960, 540, 1); // 新版推薦替換函數
	buffer = new BytePointer(av_malloc(numBytes));

	sws_ctx = sws_getContext(pCodecCtx.width(), pCodecCtx.height(), pCodecCtx.pix_fmt(), 960, 540, AV_PIX_FMT_BGR24,
		SWS_BILINEAR, null, null, (DoublePointer) null);
//  sws_ctx = sws_getContext(pCodecCtx.width(), pCodecCtx.height(),pCodecCtx.pix_fmt(), pCodecCtx.width(), pCodecCtx.height(),AV_PIX_FMT_BGR24, SWS_BILINEAR, null, null, (DoublePointer)null);
// Assign appropriate parts of buffer to image planes in pFrameRGB
// Note that pFrameRGB is an AVFrame, but AVFrame is a superset
// of AVPicture
//        avpicture_fill(new AVPicture(pFrameRGB), buffer, AV_PIX_FMT_BGR24,pCodecCtx.width(), pCodecCtx.height());
	av_image_fill_arrays(pFrameRGB.data(), pFrameRGB.linesize(), buffer, AV_PIX_FMT_BGR24, 960, 540, 1); // 新版推薦替換函數

// Read frames and save first five frames to disk
	i = 0;
//
	av_dump_format(pFormatCtx, 0, in, 0);

	Mat image = new Mat(540, 960, CV_8UC3);
//  Mat image=new Mat(pCodecCtx.height(), pCodecCtx.width(),CV_8UC3);
	while (av_read_frame(pFormatCtx, packet) >= 0) {
//pFormatCtx.fps_probe_size()
// Is this a packet from the video stream?
	    if (packet.stream_index() == videoStream) {
// Decode video frame
//                avcodec_decode_video2(pCodecCtx, pFrame, frameFinished, packet);
		int result = avcodec_send_packet(pCodecCtx, packet);
		if (result < 0) {
		    System.out.println("Decode Error1.");
		    return;
		}
		result = avcodec_receive_frame(pCodecCtx, pFrame);
		if (result < 0 && result != -11) {
		    System.out.println("Decode Error2.");
		    return;
		}
		frameFinished[0] = 1;
//  System.out.println("顯示時間戳:"+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(pFormatCtx.streams(0).cur_dts())));
// Did we get a video frame?
		if (frameFinished[0] != 0) {
// Convert the image from its native format to RGB
		    sws_scale(sws_ctx, pFrame.data(), pFrame.linesize(), 0, pCodecCtx.height(), pFrameRGB.data(),
			    pFrameRGB.linesize());

		    image.data(pFrameRGB.data(0));
		    imshow("haha", image);
		    if (waitKey(1) == 27) {
			break;
		    }
//  ShowImage(image);
// Save the frame to disk
//                    if (++i<=5) {
//                        SaveFrame(pFrameRGB, pCodecCtx.width(), pCodecCtx.height(), i);
//                    }
		}
	    }

// Free the packet that was allocated by av_read_frame 
//            av_free_packet(packet);
	    av_packet_unref(packet); // 新版推薦替換函數
	}

// Free the RGB image
	av_free(buffer);
	av_free(pFrameRGB);
// Free the YUV frame
	av_free(pFrame);
// Close the codec
	avcodec_close(pCodecCtx);
// Close the video file
	avformat_close_input(pFormatCtx);
	System.exit(0);
    }
}
