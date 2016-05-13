package jp.ac.ynu.tommylab.ecolog.drivingloggerml.uploadlog;

import java.io.File;

/**
 * 送信ファイルを扱うためにファイルオブジェクトとファイルサイズを構造体として持つクラス
 * @author 1.0 kouno作成 <br>
 *         1.1 hagimoto修正 equals()とhashcode()メソッドを追加(自作クラスでequals()などを用いるのであればこの2つのoverride()は必須)<br>
 * @version 1.1 
 */
public class FileAndLength {
	public File file;
	public long serverFileLength;

	/**
	 * コンストラクタ
	 * @param file ファイルオブジェクト
	 * @param serverFileLength ファイルサイズ
	 */
	public FileAndLength(File file, long serverFileLength){
		this.file = file;
		this.serverFileLength = serverFileLength;
	}

	/**
	 * objectのequals()のオーバーライド<br>
	 * 自作クラスをcollectionの要素とする場合<br>
	 * このメソッドがないとArratListのcontains()やindexOf()が利用できない<br>
	 */
	@Override
	public boolean equals(Object obj)
	{
		if(obj == null)
		{
			return false;
		}

		FileAndLength fl = (FileAndLength)obj;
	    File f = fl.file;

		if(file.equals(f))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * objectのhashCode()のオーバーライド<br>
	 * equals()をオーバーライドした場合hashCode()もオーバーライドする<br>
	 * 仕様<br>
	 * 1.equals()が成立する2つのオブジェクトのhashCode()の値は等しくなる<br>
	 * 2.equals()が成立しない2つのオブジェクトのhashCode()の値は等しくても等しくなくてもよい<br>
	 * (可能な限り等しくないほうが効率的)<br>
	 * 1,2を満たすような値を戻り値とするようにする<br>
	 * 最悪一定の値を返すことで実装可能<br>
	 */
	@Override
	public int hashCode()
	{
		int f;

		if(file == null)
		{
			f = 0;
		}
		else
		{
			f = file.hashCode();
		}

		int hash = f + (int)serverFileLength;

		return hash;
	}
}
