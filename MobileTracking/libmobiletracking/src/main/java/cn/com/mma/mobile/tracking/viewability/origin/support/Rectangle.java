package cn.com.mma.mobile.tracking.viewability.origin.support;

/**
 * 矩形结构体
 *
 *
 */
public class Rectangle {
	public double x1;// 左上角x点坐标(left)
	public double y1;// 左上角y点坐标(top)
	public double x2;// 右下角x点坐标(right)
	public double y2;// 右下角y点坐标(bottom)

	public String toString() {
		return "(" + x1 + "," + y1 + "," + x2 + "," + y2 + ")";
	}
}
