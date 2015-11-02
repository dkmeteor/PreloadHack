# Android�������� PreLoad Hack

�ο� [Android���������ܽ�](http://blog.zhaiyifan.cn/2015/09/10/Android%E6%8D%A2%E8%82%A4%E6%8A%80%E6%9C%AF%E6%80%BB%E7%BB%93/)

�ڲ���Դ���ط��� ��ͬС��,����ʹ�ú�ʵ��ȱ�ݷǳ���,ʵ��ʹ�ü�ֵ����.

- ���ڸ��ӵ�Ƥ��,��Ҫ̫�������.

- ���ڼ򵥵�Ƥ��(���ư���/��ҹ/�ص�ģʽ),�и��򵥵�ʵ�ַ�ʽ


��Ҫ������̬���ط���

##resource�滻
��Դ��Ŀ�ɲ���Android-Skin-Loader

���Բο����ϵ�Blog����

ʵ�ֻ�����ʵ��ʵ�ͱ���RootView�ķ������𲻴�,����Ǳ��Skin enable��,������ǵ�view

��������SkinItem,����SkinAttr,Ȼ�����skinAtrr.apply(view)������������

����Ŀ�ŵ���2��:

- ����ڱ���RootView�Ĵֱ�ʵ��,���ʵ�ֻ��ֲ�θ�����

- ����Դ�����apk,Ȼ��ͨ��AssetManager����


    PackageManager mPm = context.getPackageManager();
    PackageInfo mInfo = mPm.getPackageArchiveInfo(skinPkgPath, PackageManager.GET_ACTIVITIES);
	skinPackageName = mInfo.packageName;

	AssetManager assetManager = AssetManager.class.newInstance();
	Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
	addAssetPath.invoke(assetManager, skinPkgPath);

	Resources superRes = context.getResources();
	Resources skinResource = new Resources(assetManager,superRes.getDisplayMetrics(),superRes.getConfiguration());


ʵ�ֻ���ZYFд��2��,�����Ҹо����Ǻܶ�.

---

�Լ�����һ����ϸʵ�ֻ�������:

Android-Skin-Loader��û�и���application��getResource����.

- ʹ��ʱ����BaseActivity 

- onCreate��ʱ����� `getLayoutInflater().setFactory(mSkinInflaterFactory);`


    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSkinInflaterFactory = new SkinInflaterFactory();
		getLayoutInflater().setFactory(mSkinInflaterFactory);
	}


- ����View��Inflater������ʱ��ᾭ��mSkinInflaterFactory��onCreateView����

- ��mSkinInflaterFactory��onCreateView������,��ȡ������ص�����,���浽SkinItem������

- ������ʱ������BaseActivity��onThemeUpdate����

- onThemeUpdate�����б�������SkinItem������apply�����޸Ĳ���

	public void apply(View view) {
		
		if(RES_TYPE_NAME_COLOR.equals(attrValueTypeName)){
			view.setBackgroundColor(SkinManager.getInstance().getColor(attrValueRefId));
		}else if(RES_TYPE_NAME_DRAWABLE.equals(attrValueTypeName)){
			Drawable bg = SkinManager.getInstance().getDrawable(attrValueRefId);
			view.setBackground(bg);
		}
	}

- ��ʱ���õ�getColor��getDrawable��ͨ��AssetManager����ָ��apk�е���Դ


����������û�����ﾭ��Application,ֻ��ͨ��AssetManager��������һ��apk�е�Resource.

�ȱ���RootView��һ��ľ�������ͨ��LayoutInflater��Factoryȥ���ÿ��View�Ƿ���ҪSkinUpdate����,Ȼ����Ҫ��View��������,ThemeUpdate��ʱ��ֻˢ����ЩView.
������Ӧ���ȱ���RootView��ЧһЩ��.


## Hack Resources internally


>������ZYF��Blog


>�ڿƼ�������ֱ�Ӷ�Resources����hack��Resources.java:

    // Information about preloaded resources.  Note that they are not
    // protected by a lock, because while preloading in zygote we are all
    // single-threaded, and after that these are immutable.
    private static final LongSparseArray<Drawable.ConstantState>[] sPreloadedDrawables;
    private static final LongSparseArray<Drawable.ConstantState> sPreloadedColorDrawables
            = new LongSparseArray<Drawable.ConstantState>();
    private static final LongSparseArray<ColorStateList> sPreloadedColorStateLists
            = new LongSparseArray<ColorStateList>();
            
        
>ֱ�Ӷ�Resources�����������LongSparseArray�����滻������apk����ʱ����Դ���Ǵ�����������������صģ�����ֻҪ����interceptorģʽ��
>�Լ�ʵ��һ��LongSparseArray����ͨ������set��ȥ������ʵ�ֻ���������getDrawable�ȷ���������ôȡpreload����ģ������Լ���Resources��Դ�롣
>�ȵȣ�����ô�򵥣���NONO��������̫�����ˣ���ôȥ����xml��9patch��padding��ô���£���ô���/�����Զ����Ƥ������drawable��״̬��ôˢ�£��ȵȡ���Щ��������Ҫ���ǵģ��ڴ��ڲ����app�У�����Ҫ�����Ƿ�ụ�า��resource id�����⣬������Ҫ�޸�apt����resource id��λ����2��range��
>��Q�Ͷ�����QQ�ռ�ʹ�õ������ַ�����Ч��ͦ�á�

---

�ⷽ��Ҳû������˵��,��һ�� `�Լ�ʵ��һ��LongSparseArray` ,����ǵ���.
�����и���ʾҲ�Ǻõ�.


���ȷ���һ�¸��ֶο���������ʲô����

        Resources resource = getApplicationContext().getResources();

        try {
        Field field =Resources.class.getDeclaredField("sPreloadedDrawables");
        field.setAccessible(true);

        LongSparseArray<Drawable.ConstantState>[]    sPreloadedDrawables = (LongSparseArray<Drawable.ConstantState>[] )field.get(resource);

        for (LongSparseArray<Drawable.ConstantState> s:sPreloadedDrawables)
            for (int i = 0; i < s.size(); i++) {
                System.out.println(s.valueAt(i));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }



---
  

    ...
    10-28 20:15:54.105 24502-24502/com.dk_exp.preloadhack I/System.out: android.graphics.drawable.LayerDrawable$LayerState@8197fd4
    10-28 20:15:54.105 24502-24502/com.dk_exp.preloadhack I/System.out: android.graphics.drawable.LayerDrawable$LayerState@9c6357d
    10-28 20:15:54.105 24502-24502/com.dk_exp.preloadhack I/System.out: android.graphics.drawable.LayerDrawable$LayerState@95f0c72
    10-28 20:15:54.105 24502-24502/com.dk_exp.preloadhack I/System.out: android.graphics.drawable.StateListDrawable$StateListState@d78c540
    10-28 20:15:54.105 24502-24502/com.dk_exp.preloadhack I/System.out: android.graphics.drawable.StateListDrawable$StateListState@805ac79
    10-28 20:15:54.105 24502-24502/com.dk_exp.preloadhack I/System.out: android.graphics.drawable.BitmapDrawable$BitmapState@38d7dbe
    10-28 20:15:54.105 24502-24502/com.dk_exp.preloadhack I/System.out: android.graphics.drawable.StateListDrawable$StateListState@e829e1f
    10-28 20:15:54.105 24502-24502/com.dk_exp.preloadhack I/System.out: android.graphics.drawable.StateListDrawable$StateListState@4c4f56c
    10-28 20:15:54.105 24502-24502/com.dk_exp.preloadhack I/System.out: android.graphics.drawable.VectorDrawable$VectorDrawableState@82b9735
    10-28 20:15:54.105 24502-24502/com.dk_exp.preloadhack I/System.out: android.graphics.drawable.VectorDrawable$VectorDrawableState@a9fb7ca
    ...

���Կ���sPreloadedDrawables����д�����State����,����`BitmapDrawable$BitmapState`


��ΪBitmapDrawable���ڲ���,���ǱȽϼ򵥵�,��һ����������

    final static class BitmapState extends ConstantState {
        final Paint mPaint;

        // Values loaded during inflation.
        int[] mThemeAttrs = null;
        Bitmap mBitmap = null;
        ColorStateList mTint = null;
        Mode mTintMode = DEFAULT_TINT_MODE;
        int mGravity = Gravity.FILL;
        float mBaseAlpha = 1.0f;
        Shader.TileMode mTileModeX = null;
        Shader.TileMode mTileModeY = null;
        int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
        boolean mAutoMirrored = false;

        int mChangingConfigurations;
        boolean mRebuildShader;

        BitmapState(Bitmap bitmap) {
            mBitmap = bitmap;
            mPaint = new Paint(DEFAULT_PAINT_FLAGS);
        }

        BitmapState(BitmapState bitmapState) {
            mBitmap = bitmapState.mBitmap;
            mTint = bitmapState.mTint;
            mTintMode = bitmapState.mTintMode;
            mThemeAttrs = bitmapState.mThemeAttrs;
            mChangingConfigurations = bitmapState.mChangingConfigurations;
            mGravity = bitmapState.mGravity;
            mTileModeX = bitmapState.mTileModeX;
            mTileModeY = bitmapState.mTileModeY;
            mTargetDensity = bitmapState.mTargetDensity;
            mBaseAlpha = bitmapState.mBaseAlpha;
            mPaint = new Paint(bitmapState.mPaint);
            mRebuildShader = bitmapState.mRebuildShader;
            mAutoMirrored = bitmapState.mAutoMirrored;
        }

        @Override
        public boolean canApplyTheme() {
            return mThemeAttrs != null || mTint != null && mTint.canApplyTheme();
        }

        @Override
        public int addAtlasableBitmaps(Collection<Bitmap> atlasList) {
            if (isAtlasable(mBitmap) && atlasList.add(mBitmap)) {
                return mBitmap.getWidth() * mBitmap.getHeight();
            }
            return 0;
        }

        @Override
        public Drawable newDrawable() {
            return new BitmapDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new BitmapDrawable(this, res);
        }

        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations
                    | (mTint != null ? mTint.getChangingConfigurations() : 0);
        }
    }

      
      
�����Ѿ���������`sPreloadedDrawables` ,��ô��취�޸�sPreloadedDrawables��Ķ���Ӧ���Ϳ����޸� ͼƬ ��Դ��.

Ȼ�������˶������

- ����`BitmapState`�������޷�����,������Drawable.ConstantState��û���ṩ�޸ĵĽӿ�.

- ϡ�������key������ResourceId  
 

    key = (((long) value.assetCookie) << 32) | value.data;


׷��һ�µ���ջ,���value��������һ��native����,��ʱ��������assetCookie��data�ļ��㷽��

    private native final int loadResourceValue(int ident, short density, TypedValue outValue,
            boolean resolve);
            


����Resource�����ṩgetVaklue��������TypeValue�������

     public void getValue(@AnyRes int id, TypedValue outValue, boolean resolveRefs)


��ô�ҿ��Գ���ֱ��ͨ��TypeValue������preload�е�����

        TypedValue value = new TypedValue();
        resource.getValue(R.drawable.charming,value,true );

        long  key = -1;
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            key = value.data;
        } else {
            key = (((long) value.assetCookie) << 32) | value.data;
        }
        Drawable.ConstantState cs =sPreloadedDrawable.get(key);

Ȼ������������һ������,��ȡTypeValue�ͼ���key��������

����ͨ��key��ȡConstantState������null.

�ϵ���Լ����LongSparseArray�е�key����,ȷʵû�ж�Ӧ��ֵ

���Ե�ʱ��ע�⵽����һ������

key = 8589934596

��LongSparseArray�е�����keyΪ

4294967922  
4294967923  
...


����ԱӦ���������ֱȽ�����

8589934596 = 0x200000004

4294967922 = 0x100000272

���Ϸ�key�ļ����߼����Ƶ�,���Կ�����assertCookie��ͬ

�������ҷ������sPreloadedDrawables�в���һ����������Ҫ���ҵ���Դ

---

����Resource.loadDrawable��Դ��,����drawabelҲ�����Ǵ�mDrawableCache�л�ȡ��

��ش���:

        if (!mPreloading) {
            final Drawable cachedDrawable = caches.getInstance(key, theme);
            if (cachedDrawable != null) {
                return cachedDrawable;
            }
        }

���DrawableCache�౾��ֻ�а�����Ȩ��,������뻹Ҫдһ��,����Debugģʽ�¿���ֱ����resource�￴���������

DemoӦ����drawable�ļ�����ֻ��2����Դ,һ����������ȥ�Ĳ���ͼƬ,һ�ŵ�ic_launch

�����һ������е�keys��,��Ȼ�ҵ���8589934596.

������һ�����Է���mDrawableCache���޸���������.


ע��һ������.��� `android.content.res.DrawableCache` ��,ֻ�а�����Ȩ��

����ʹ��Class.forName("android.content.res.DrawableCache")����

---

�����ҷ��˸�����,�ҵ���ʱʹ�õ�genymotionģ������5.0.1�� 

��API21�汾�� drawableCache��ʵ���ǲ�ͬ��

API21

    private final ArrayMap<String, LongSparseArray<WeakReference<ConstantState>>> mDrawableCache =
             new ArrayMap<String, LongSparseArray<WeakReference<ConstantState>>>();



API23

    private final DrawableCache mDrawableCache = new DrawableCache(this);
    
    
��Ϊ���ԭ��,�ڷ���������˷���һЩʱ��,�Ժ�Ӧ��ע���������.
�о�Դ����صĶ���ʱ,һ��Ҫʹ����ͬ�汾���豸/ģ����,��Ȼ��ȫ���˷�ʱ��.

---

����6.0�豸������һ��,�ɹ��õ�������Ҫ��Drawable����

��������


       Resources resource = getApplicationContext().getResources();
        Object mdrawableCache = null;
        Field field = null;
        try {
            field = Resources.class.getDeclaredField("mDrawableCache");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        field.setAccessible(true);
        try {
            mdrawableCache = field.get(resource);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        TypedValue value = new TypedValue();
        resource.getValue(R.drawable.charming,value,true );

        long  key = -1;
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            key = value.data;
        } else {
            key = (((long) value.assetCookie) << 32) | value.data;
        }

        Method method = null;
        try {
            Class  c = mdrawableCache.getClass();
            method = c.getDeclaredMethod("getInstance",long.class,Resources.Theme.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        Drawable drawable = null;
        try {
            drawable = (Drawable) method.invoke(mdrawableCache, key, null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


---

���濼���滻��Drawable��ˢ��View,�ο�ThemedResourceCacheԴ��,�²���Ե���put�������޸ĺ��Drawable��������ȥ.

����ThemeResourceCache���е�ʵ���ϻ���Drawable.ConstantState����,Drawable��������newDrawable()������ȡ,����Ӧ������BitmapState����

������Ȼ�ǳ�����,BitmapState��BitmapDrawable�ľ�̬�ڲ���,default,ֻ�а�����Ȩ��.

�����ǹ������,���÷���,�޸Ĳ���,����Ҫͨ������,�о�����Ƿǳ��ǳ��鷳.

---

���о������п�,��Ϊ����Resource����DrawableCache��Preload��ʵ��,����5.0��6.0��ʵ���߼��ֲ�ͬ.

ͨ������hack cache������Դ�滻������������һ�����׵ķ���.





























