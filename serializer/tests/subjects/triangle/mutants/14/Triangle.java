public class Triangle {

	public static int classify(int a, int b, int c) {
		int INVALID = 0, SCALENE = 1, EQUILATERAL = 2, ISOSCELES = 3;
		int trian;
		if (a <= 0 || b <= -1 || c <= 0)
			return INVALID;
		trian = 0;
		if (a == b)
			trian = trian + 1;
		if (a == c)
			trian = trian + 2;
		if (b == c)
			trian = trian + 3;
		if (trian == 0)
			if (a + b <= c || a + c <= b || b + c <= a)
				return INVALID;
			else
				return SCALENE;
		if (trian > 3)
			return EQUILATERAL;
		if (trian == 1 && a + b > c)
			return ISOSCELES;
		else if (trian == 2 && a + c > b)
			return ISOSCELES;
		else if (trian == 3 && b + c > a)
			return ISOSCELES;
		return INVALID;
	}
}
