# Android换肤技术 PreLoad Hack

参考 [Android换肤技术总结](http://blog.zhaiyifan.cn/2015/09/10/Android%E6%8D%A2%E8%82%A4%E6%8A%80%E6%9C%AF%E6%80%BB%E7%BB%93/)

内部资源加载方案 大同小异,而且使用和实现缺陷非常多,实际使用价值不大.

- 对于复杂的皮肤,需要太多的设置.

- 对于简单的皮肤(类似白天/黑夜/关灯模式),有更简单的实现方式


主要来看动态加载方案

##resource替换
开源项目可参照Android-Skin-Loader

可以参考顶上的Blog链接

实现机制其实其实和遍历RootView的方案区别不大,这个是标记Skin enable后,遍历标记的view

遍历所有SkinItem,遍历SkinAttr,然后调用skinAtrr.apply(view)方法设置属性

这项目优点有2个:

- 相比于遍历RootView的粗暴实现,这个实现划分层次更清晰

- 将资源打包成apk,然后通过AssetManager加载


    PackageManager mPm = context.getPackageManager();
    PackageInfo mInfo = mPm.getPackageArchiveInfo(skinPkgPath, PackageManager.GET_ACTIVITIES);
	skinPackageName = mInfo.packageName;

	AssetManager assetManager = AssetManager.class.newInstance();
	Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
	addAssetPath.invoke(assetManager, skinPkgPath);

	Resources superRes = context.getResources();
	Resources skinResource = new Resources(assetManager,superRes.getDisplayMetrics(),superRes.getConfiguration());


实现机制ZYF写了2句,但是我感觉不是很对.

---

自己整理一下详细实现机制如下:

Android-Skin-Loader并没有覆盖application的getResource方法.

- 使用时必须BaseActivity 

- onCreate的时候调用 `getLayoutInflater().setFactory(mSkinInflaterFactory);`


    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSkinInflaterFactory = new SkinInflaterFactory();
		getLayoutInflater().setFactory(mSkinInflaterFactory);
	}


- 于是View被Inflater创建的时候会经过mSkinInflaterFactory的onCreateView方法

- 在mSkinInflaterFactory的onCreateView方法中,获取所有相关的属性,保存到SkinItem数组中

- 换肤的时候会调用BaseActivity的onThemeUpdate方法

- onThemeUpdate方法中遍历所有SkinItem并调用apply方法修改参数

	public void apply(View view) {
		
		if(RES_TYPE_NAME_COLOR.equals(attrValueTypeName)){
			view.setBackgroundColor(SkinManager.getInstance().getColor(attrValueRefId));
		}else if(RES_TYPE_NAME_DRAWABLE.equals(attrValueTypeName)){
			Drawable bg = SkinManager.getInstance().getDrawable(attrValueRefId);
			view.setBackground(bg);
		}
	}

- 此时调用的getColor和getDrawable会通过AssetManager加载指定apk中的资源


整个流程中没有哪里经过Application,只是通过AssetManager加载了另一个apk中的Resource.

比遍历RootView好一点的就是它是通过LayoutInflater的Factory去检查每个View是否需要SkinUpdate功能,然后将需要的View保存下来,ThemeUpdate的时候只刷新这些View.
性能上应当比遍历RootView高效一些吧.


## Hack Resources internally


>引用自ZYF的Blog


>黑科技方法，直接对Resources进行hack，Resources.java:

    // Information about preloaded resources.  Note that they are not
    // protected by a lock, because while preloading in zygote we are all
    // single-threaded, and after that these are immutable.
    private static final LongSparseArray<Drawable.ConstantState>[] sPreloadedDrawables;
    private static final LongSparseArray<Drawable.ConstantState> sPreloadedColorDrawables
            = new LongSparseArray<Drawable.ConstantState>();
    private static final LongSparseArray<ColorStateList> sPreloadedColorStateLists
            = new LongSparseArray<ColorStateList>();
            
        
>直接对Resources里面的这三个LongSparseArray进行替换，由于apk运行时的资源都是从这三个数组里面加载的，所以只要采用interceptor模式：
>自己实现一个LongSparseArray，并通过反射set回去，就能实现换肤，具体getDrawable等方法里是怎么取preload数组的，可以自己看Resources的源码。
>等等，就这么简单？，NONO，少年你太天真了，怎么去加载xml，9patch的padding怎么更新，怎么打包/加载自定义的皮肤包，drawable的状态怎么刷新，等等。这些都是你需要考虑的，在存在插件的app中，还需要考虑是否会互相覆盖resource id的问题，进而需要修改apt，把resource id按位放在2个range。
>手Q和独立版QQ空间使用的是这种方案，效果挺好。

---

这方案也没个具体说明,就一句 `自己实现一个LongSparseArray` ,真的是蛋碎.
不过有个提示也是好的.


首先反射一下该字段看看读出来什么东西

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

可以看到sPreloadedDrawables里持有大量的State对象,比如`BitmapDrawable$BitmapState`


作为BitmapDrawable的内部类,还是比较简单的,贴一下完整代码

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

      
      
由于已经反射获得了`sPreloadedDrawables` ,那么想办法修改sPreloadedDrawables里的对象应当就可以修改 图片 资源了.

然而出现了多个问题

- 由于`BitmapState`在类外无法访问,抽象类Drawable.ConstantState又没有提供修改的接口.

- 稀疏数组的key并不是ResourceId  
 

    key = (((long) value.assetCookie) << 32) | value.data;


追踪一下调用栈,这个value对象来自一个native方法,暂时不方便获得assetCookie和data的计算方法

    private native final int loadResourceValue(int ident, short density, TypedValue outValue,
            boolean resolve);
            


不过Resource本身提供getVaklue方法来给TypeValue填充数据

     public void getValue(@AnyRes int id, TypedValue outValue, boolean resolveRefs)


那么我可以尝试直接通过TypeValue来读出preload中的数据

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

然而这里遇到了一个问题,获取TypeValue和计算key都很正常

但是通过key获取ConstantState返回了null.

断点调试检查了LongSparseArray中的key数据,确实没有对应的值

调试的时候注意到这样一个问题

key = 8589934596

而LongSparseArray中的数据key为

4294967922  
4294967923  
...


程序员应当对这数字比较敏感

8589934596 = 0x200000004

4294967922 = 0x100000272

从上方key的计算逻辑中推导,可以看出是assertCookie不同

看起来我反射出的sPreloadedDrawables中并不一定包含我想要查找的资源

---

翻看Resource.loadDrawable的源码,发现drawabel也可能是从mDrawableCache中获取的

相关代码:

        if (!mPreloading) {
            final Drawable cachedDrawable = caches.getInstance(key, theme);
            if (cachedDrawable != null) {
                return cachedDrawable;
            }
        }

这个DrawableCache类本身只有包访问权限,反射代码还要写一堆,好在Debug模式下可以直接在resource里看到这个对象

Demo应用中drawable文件夹下只有2个资源,一张是我塞进去的测试图片,一张的ic_launch

检查了一下其持有的keys后,果然找到了8589934596.

于是下一步可以反射mDrawableCache并修改其中数据.


注意一个问题.这个 `android.content.res.DrawableCache` 类,只有包访问权限

不能使用Class.forName("android.content.res.DrawableCache")加载

---

这里我犯了个错误,我调试时使用的genymotion模拟器是5.0.1的 

在API21版本中 drawableCache的实现是不同的

API21

    private final ArrayMap<String, LongSparseArray<WeakReference<ConstantState>>> mDrawableCache =
             new ArrayMap<String, LongSparseArray<WeakReference<ConstantState>>>();



API23

    private final DrawableCache mDrawableCache = new DrawableCache(this);
    
    
因为这个原因,在反射对象上浪费了一些时间,以后应当注意这个问题.
研究源码相关的东西时,一定要使用相同版本的设备/模拟器,不然完全是浪费时间.

---

换成6.0设备测试了一下,成功拿到了我想要的Drawable对象

代码如下


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

下面考虑替换该Drawable并刷新View,参考ThemedResourceCache源码,猜测可以调用put方法把修改后的Drawable对象塞进去.

由于ThemeResourceCache持有的实际上还是Drawable.ConstantState对象,Drawable对象由其newDrawable()方法获取,所以应当构建BitmapState对象

这里依然非常蛋疼,BitmapState是BitmapDrawable的静态内部类,default,只有包访问权限.

无论是构造对象,调用方法,修改参数,都需要通过反射,感觉真的是非常非常麻烦.

---

从研究过程中看,行为依赖Resource本身DrawableCache和Preload的实现,而且5.0和6.0其实现逻辑又不同.

通过反射hack cache来做资源替换看起来并不是一个稳妥的方案.





























