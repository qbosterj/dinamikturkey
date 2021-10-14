package cn.com.mma.mobile.tracking.viewability.origin.support;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

/**
 *
 *
 */
public class AtlantisUtil {

	private SegTree st = new SegTree(310);
	private edge[] ed = new edge[310];
	private zone[] zn = new zone[310];
	private int len;
	private int cnt;
	private double hash[] = new double[310];

	public AtlantisUtil() {
		for (int i = 0; i <= 300; i += 2) {
			ed[i] = new edge();
			ed[i + 1] = new edge();
			zn[i] = new zone();
			zn[i + 1] = new zone();
		}
	}

	private void build() {
		Arrays.sort(zn, 1, len * 2 + 1);
		cnt = 1;
		for (int i = 1; i <= 2 * len; i++) {
			if (i > 1 && zn[i].h != zn[i - 1].h)
				cnt++;
			hash[cnt] = zn[i].h;
			int temp = zn[i].id;
			if (temp > 0)
				ed[temp].s = ed[temp + 1].s = cnt;
			else
				ed[-temp].t = ed[-temp + 1].t = cnt;
		}
	}

	private void fill(List<Rectangle> rectList) {

		int i = 1;// i=1代表矩形的顶部边线 i=-1代表矩形的底部边线
		for (Rectangle rectangle : rectList) {
			ed[i].x = rectangle.x1;
			ed[i].v = 1;
			//System.out.println("ed[i].x:" + ed[i].x + "  ed[i].v:" + ed[i].v);
			zn[i].id = i;
			zn[i].h = rectangle.y1;
			//System.out.println("zn[i].id :" + zn[i].id + "  zn[i].h:" + zn[i].h);
			ed[i + 1].x = rectangle.x2;
			ed[i + 1].v = -1;
			//System.out.println("ed[i+1].x  :" + ed[i + 1].x + "  ed[i+1].v:" + ed[i + 1].v);
			zn[i + 1].id = -i;
			zn[i + 1].h = rectangle.y2;
			//System.out.println("zn[i+1].id  :" + zn[i + 1].id + "  zn[i+1].h:" + zn[i + 1].h);
			//System.out.println("======================================");
			i += 2;
		}
	}

	public double calOverlapArea(List<Rectangle> rectList) {
		double ans = 0.0;
		try {
			long start = System.currentTimeMillis();
			len = rectList.size();
			fill(rectList);
			build();
			Arrays.sort(ed, 1, len * 2 + 1);
			st.init(1, cnt - 1, 1);
			st.update(ed[1].s, ed[1].t - 1, 1, 1);
			for (int j = 2; j <= len * 2; j++) {
				ans += st.tree[1].sum * (ed[j].x - ed[j - 1].x);
				//System.out.println("ans:" + ans);
				st.update(ed[j].s, ed[j].t - 1, 1, ed[j].v);
			}
			long end = System.currentTimeMillis();
			DecimalFormat df = new DecimalFormat("0.00");
//			System.out.println("Total explored area: " + df.format(ans) + "  cost:" + (end - start) + " ms");
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return ans;
	}

	private class edge implements Comparable<edge> {
		int s, t, v;
		double x;

		public int compareTo(edge oth) {
			if (x < oth.x)
				return -1;
			if (x == oth.x && v > oth.v)
				return -1;
			// 在求矩形周长并时发现的错误，貌似面积并沒问题，也许是数据太弱的缘故，先写上，稍后研究
			return 1;
		}
	}

	private class zone implements Comparable<zone> {
		int id;
		double h;

		public int compareTo(zone o) {
			if (h < o.h)
				return -1;
			return 1;
		}
	}

	private class SegTree {
		node tree[];

		public SegTree(int maxn) {
			maxn = maxn * 3;
			tree = new node[maxn];
			for (int i = 1; i < maxn; i++)
				tree[i] = new node();
		}

		class node {
			int left, right, key;
			double sum;

			void init(int l, int r) {
				left = l;
				right = r;
				sum = 0;
				key = 0;
			}

			int mid() {
				return (left + right) >> 1;
			}

			double length() {
				return hash[right + 1] - hash[left];
			}
		}

		void init(int l, int r, int idx) {
			tree[idx].init(l, r);
			if (l == r)
				return;
			int mid = tree[idx].mid();
			init(l, mid, idx << 1);
			init(mid + 1, r, (idx << 1) | 1);
		}

		void update(int left, int right, int idx, int v) {
			if (tree[idx].left >= left && tree[idx].right <= right) {
				tree[idx].key += v;
				pushup(idx);
				return;
			}
			int mid = tree[idx].mid();
			if (left <= mid)
				update(left, right, idx << 1, v);
			if (right > mid)
				update(left, right, (idx << 1) | 1, v);
			pushup(idx);
		}

		void pushup(int idx) {
			if (tree[idx].key > 0)
				tree[idx].sum = tree[idx].length();
			else {
				if (tree[idx].left == tree[idx].right)
					tree[idx].sum = 0;
				else
					tree[idx].sum = tree[idx << 1].sum + tree[(idx << 1) | 1].sum;
			}
		}
	}

}
