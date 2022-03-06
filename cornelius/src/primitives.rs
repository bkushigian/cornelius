use std::str::FromStr;
use std::fmt;
use std::fmt::Formatter;
use std::ops;
use std::num::Wrapping;
use std::convert::From;

#[derive(Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct JavaInt(i32);

#[derive(Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct JavaLong(i64);

impl From<i32> for JavaInt {
    fn from(x: i32) -> Self {
        JavaInt(x)
    }
}

impl From<i64> for JavaLong {
    fn from(x: i64) -> Self {
        JavaLong(x as i64)
    }
}

pub trait IsZero {
    fn is_zero(&self) -> bool;
}

impl IsZero for JavaLong {
    fn is_zero(&self) -> bool {
        self.0 == 0
    }
}

impl IsZero for JavaInt {
    fn is_zero(&self) -> bool {
        self.0 == 0
    }
}

impl FromStr for JavaLong {
    type Err = String;
    fn from_str(s: &str) -> Result<Self, Self::Err> {
        return match s.strip_suffix(|c| c == 'l' || c == 'L') {
            None =>  Err("JavaLong literals must end with `l` or `L`".to_string()),
            Some(s) => match s.parse::<i64>() {
                Ok(n) => Ok(JavaLong(n)),
                Err(_) => Err("Error parsing as i64".to_string())
            }
        }
    }
}

impl fmt::Display for JavaLong {
    fn fmt(&self, f: &mut Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

impl FromStr for JavaInt {
    type Err = String;
    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s.parse::<i32>() {
            Ok(n) => Ok(JavaInt(n)),
            Err(_) => Err("Error parsing as i32".to_string())
        }
    }
}

impl PartialEq<JavaInt> for JavaLong {
    fn eq(&self, other: &JavaInt) -> bool {
        other.0 as i64 == self.0
    }
}

impl PartialEq<JavaLong> for JavaInt {
    fn eq(&self, other: &JavaLong) -> bool {
        other.0 == (self.0 as i64)
    }
}

impl fmt::Display for JavaInt {
    fn fmt(&self, f: &mut Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

impl JavaInt {
    pub fn promote_to_java_long(&self) -> JavaLong {
        JavaLong(self.0 as i64)
    }
}


/*   Operators   */
/*      ~~~ Ops for JavaLong ~~~      */


impl ops::Add<JavaLong> for JavaLong {
    type Output = JavaLong;
    fn add(self, rhs: JavaLong) -> Self::Output {
        JavaLong(Wrapping(self.0 + rhs.0).0)
    }
}

impl ops::Add<JavaInt> for JavaLong {
    type Output = JavaLong;
    fn add(self, rhs: JavaInt) -> Self::Output {
        JavaLong(Wrapping(self.0 + (rhs.0 as i64)).0)
    }
}

impl ops::Sub<JavaLong> for JavaLong {
    type Output = JavaLong;
    fn sub(self, rhs: JavaLong) -> Self::Output {
        JavaLong(Wrapping(self.0 - rhs.0).0)
    }
}

impl ops::Sub<JavaInt> for JavaLong {
    type Output = JavaLong;
    fn sub(self, rhs: JavaInt) -> Self::Output {
        JavaLong(Wrapping(self.0 - (rhs.0 as i64)).0)
    }
}

impl ops::Mul<JavaLong> for JavaLong {
    type Output = JavaLong;
    fn mul(self, rhs: JavaLong) -> Self::Output {
        JavaLong(Wrapping(self.0 * rhs.0).0)
    }
}

impl ops::Mul<JavaInt> for JavaLong {
    type Output = JavaLong;
    fn mul(self, rhs: JavaInt) -> Self::Output {
        JavaLong(Wrapping(self.0 * (rhs.0 as i64)).0)
    }
}

impl ops::Div<JavaLong> for JavaLong {
    type Output = JavaLong;
    fn div(self, rhs: JavaLong) -> Self::Output {
        JavaLong(Wrapping(self.0 / rhs.0).0)
    }
}

impl ops::Div<JavaInt> for JavaLong {
    type Output = JavaLong;
    fn div(self, rhs: JavaInt) -> Self::Output {
        JavaLong(Wrapping(self.0 / (rhs.0 as i64)).0)
    }
}

impl ops::Rem<JavaLong> for JavaLong {
    type Output = JavaLong;
    fn rem(self, rhs: JavaLong) -> Self::Output {
        JavaLong(Wrapping(self.0 % rhs.0).0)
    }
}

impl ops::Rem<JavaInt> for JavaLong {
    type Output = JavaLong;
    fn rem(self, rhs: JavaInt) -> Self::Output {
        JavaLong(Wrapping(self.0 % (rhs.0 as i64)).0)
    }
}

impl ops::Shl<JavaLong> for JavaLong {
    type Output = JavaLong;
    fn shl(self, rhs: JavaLong) -> Self::Output {
        JavaLong(self.0 << rhs.0)
    }
}

impl ops::Shl<JavaInt> for JavaLong {
    type Output = JavaLong;
    fn shl(self, rhs: JavaInt) -> Self::Output {
        JavaLong(self.0 << (rhs.0 as i64))
    }
}

impl ops::Shr<JavaLong> for JavaLong {
    type Output = JavaLong;
    fn shr(self, rhs: JavaLong) -> Self::Output {
        JavaLong(self.0 >> rhs.0)
    }
}

impl ops::Shr<JavaInt> for JavaLong {
    type Output = JavaLong;
    fn shr(self, rhs: JavaInt) -> Self::Output {
        JavaLong(self.0 >> (rhs.0 as i64))
    }
}

impl ops::BitAnd<JavaLong> for JavaLong {
    type Output = JavaLong;
    fn bitand(self, rhs: JavaLong) -> Self::Output {
        JavaLong(self.0 & rhs.0)
    }
}

impl ops::BitAnd<JavaInt> for JavaLong {
    type Output = JavaLong;
    fn bitand(self, rhs: JavaInt) -> Self::Output {
        JavaLong(self.0 & (rhs.0 as i64))
    }
}

impl ops::BitOr<JavaLong> for JavaLong {
    type Output = JavaLong;
    fn bitor(self, rhs: JavaLong) -> Self::Output {
        JavaLong(self.0 | rhs.0)
    }
}

impl ops::BitOr<JavaInt> for JavaLong {
    type Output = JavaLong;
    fn bitor(self, rhs: JavaInt) -> Self::Output {
        JavaLong(self.0 | (rhs.0 as i64))
    }
}

impl ops::BitXor<JavaLong> for JavaLong {
    type Output = JavaLong;
    fn bitxor(self, rhs: JavaLong) -> Self::Output {
        JavaLong(self.0 ^ rhs.0)
    }
}

impl ops::BitXor<JavaInt> for JavaLong {
    type Output = JavaLong;
    fn bitxor(self, rhs: JavaInt) -> Self::Output {
        JavaLong(self.0 ^ (rhs.0 as i64))
    }
}

impl ops::Neg for JavaLong {
    type Output = JavaLong;
    fn neg(self) -> Self::Output {
        JavaLong(- self.0)
    }
}


/*      ~~~ Ops for JavaInt ~~~      */

impl ops::Add<JavaInt> for JavaInt {
    type Output = JavaInt;
    fn add(self, rhs: JavaInt) -> Self::Output {
        JavaInt((Wrapping(self.0) + Wrapping(rhs.0)).0)
    }
}

impl ops::Add<JavaLong> for JavaInt {
    type Output = JavaLong;
    fn add(self, rhs: JavaLong) -> Self::Output {
        JavaLong((Wrapping(self.0 as i64) + Wrapping(rhs.0)).0)
    }
}

impl ops::Sub<JavaInt> for JavaInt {
    type Output = JavaInt;
    fn sub(self, rhs: JavaInt) -> Self::Output {
        JavaInt((Wrapping(self.0) - Wrapping(rhs.0)).0)
    }
}

impl ops::Sub<JavaLong> for JavaInt {
    type Output = JavaLong;
    fn sub(self, rhs: JavaLong) -> Self::Output {
        JavaLong((Wrapping(self.0 as i64) - Wrapping(rhs.0)).0)
    }
}

impl ops::Mul<JavaInt> for JavaInt {
    type Output = JavaInt;
    fn mul(self, rhs: JavaInt) -> Self::Output {
        JavaInt((Wrapping(self.0) * Wrapping(rhs.0)).0)
    }
}

impl ops::Mul<JavaLong> for JavaInt {
    type Output = JavaLong;
    fn mul(self, rhs: JavaLong) -> Self::Output {
        JavaLong((Wrapping(self.0 as i64) * Wrapping(rhs.0)).0)
    }
}

impl ops::Div<JavaInt> for JavaInt {
    type Output = JavaInt;
    fn div(self, rhs: JavaInt) -> Self::Output {
        JavaInt((Wrapping(self.0) / Wrapping(rhs.0)).0)
    }
}

impl ops::Div<JavaLong> for JavaInt {
    type Output = JavaLong;
    fn div(self, rhs: JavaLong) -> Self::Output {
        JavaLong((Wrapping(self.0 as i64) / Wrapping(rhs.0)).0)
    }
}

impl ops::Rem<JavaInt> for JavaInt {
    type Output = JavaInt;
    fn rem(self, rhs: JavaInt) -> Self::Output {
        JavaInt(self.0 % rhs.0)
    }
}

impl ops::Rem<JavaLong> for JavaInt {
    type Output = JavaLong;
    fn rem(self, rhs: JavaLong) -> Self::Output {
        JavaLong((self.0 as i64) % rhs.0)
    }
}

impl ops::Shl<JavaInt> for JavaInt {
    type Output = JavaInt;
    fn shl(self, rhs: JavaInt) -> Self::Output {
        JavaInt(self.0 << rhs.0)
    }
}

impl ops::Shl<JavaLong> for JavaInt {
    type Output = JavaLong;
    fn shl(self, rhs: JavaLong) -> Self::Output {
        JavaLong((self.0 as i64) << rhs.0)
    }
}

impl ops::Shr<JavaInt> for JavaInt {
    type Output = JavaInt;
    fn shr(self, rhs: JavaInt) -> Self::Output {
        JavaInt(self.0 >> rhs.0)
    }
}

impl ops::Shr<JavaLong> for JavaInt {
    type Output = JavaLong;
    fn shr(self, rhs: JavaLong) -> Self::Output {
        JavaLong((self.0 as i64) >> rhs.0)
    }
}

impl ops::BitAnd<JavaInt> for JavaInt {
    type Output = JavaInt;
    fn bitand(self, rhs: JavaInt) -> Self::Output {
        JavaInt(self.0 & rhs.0)
    }
}

impl ops::BitAnd<JavaLong> for JavaInt {
    type Output = JavaLong;
    fn bitand(self, rhs: JavaLong) -> Self::Output {
        JavaLong((self.0 as i64) & rhs.0)
    }
}

impl ops::BitOr<JavaInt> for JavaInt {
    type Output = JavaInt;
    fn bitor(self, rhs: JavaInt) -> Self::Output {
        JavaInt(self.0 | rhs.0)
    }
}

impl ops::BitOr<JavaLong> for JavaInt {
    type Output = JavaLong;
    fn bitor(self, rhs: JavaLong) -> Self::Output {
        JavaLong((self.0 as i64) | rhs.0)
    }
}

impl ops::BitXor<JavaInt> for JavaInt {
    type Output = JavaInt;
    fn bitxor(self, rhs: JavaInt) -> Self::Output {
        JavaInt(self.0 ^ rhs.0)
    }
}

impl ops::BitXor<JavaLong> for JavaInt {
    type Output = JavaLong;
    fn bitxor(self, rhs: JavaLong) -> Self::Output {
        JavaLong((self.0 as i64) ^ rhs.0)
    }
}

impl ops::Neg for JavaInt {
    type Output = JavaInt;
    fn neg(self) -> Self::Output {
        JavaInt(- self.0)
    }
}
