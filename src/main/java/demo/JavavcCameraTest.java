package demo;

import javax.swing.JFrame;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameGrabber;

//java通過javacv獲取windows的攝像頭視訊
public class JavavcCameraTest {
    public static void main(String[] args) throws Exception, InterruptedException {
	OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
	grabber.start(); // 開始獲取攝像頭資料
	CanvasFrame canvas = new CanvasFrame("攝像頭");// 新建一個視窗
	canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	canvas.setAlwaysOnTop(true);
	while (true) {
	    if (!canvas.isDisplayable()) {// 視窗是否關閉
		grabber.stop();// 停止抓取
		System.exit(2);// 退出
	    }
	    canvas.showImage(grabber.grab());// 獲取攝像頭影象並放到視窗上顯示， 這裡的Frame frame=grabber.grab(); frame是一幀視訊影象
	    Thread.sleep(50);// 50毫秒重新整理一次影象
	}
    }
}
