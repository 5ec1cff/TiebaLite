package com.huanchengfly.tieba.post.api.retrofit

import android.os.Build
import com.huanchengfly.tieba.post.BaseApplication
import com.huanchengfly.tieba.post.api.Header
import com.huanchengfly.tieba.post.api.Param
import com.huanchengfly.tieba.post.api.models.OAID
import com.huanchengfly.tieba.post.api.retrofit.adapter.DeferredCallAdapterFactory
import com.huanchengfly.tieba.post.api.retrofit.adapter.FlowCallAdapterFactory
import com.huanchengfly.tieba.post.api.retrofit.converter.gson.GsonConverterFactory
import com.huanchengfly.tieba.post.api.retrofit.interceptors.*
import com.huanchengfly.tieba.post.api.retrofit.interfaces.MiniTiebaApi
import com.huanchengfly.tieba.post.api.retrofit.interfaces.NewTiebaApi
import com.huanchengfly.tieba.post.api.retrofit.interfaces.OfficialTiebaApi
import com.huanchengfly.tieba.post.api.retrofit.interfaces.WebTiebaApi
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.CuidUtils
import com.huanchengfly.tieba.post.utils.MobileInfoUtil
import com.huanchengfly.tieba.post.utils.UIDUtil
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.protobuf.ProtoConverterFactory


object RetrofitTiebaApi {
    private val initTime = System.currentTimeMillis()
    private val clientId = "wappc_${initTime}_${Math.round(Math.random() * 1000).toInt()}"
    private val stParamInterceptor = StParamInterceptor()
    private val connectionPool = ConnectionPool()

    private val defaultCommonParamInterceptor = CommonParamInterceptor(
        Param.BDUSS to { AccountUtil.getBduss(BaseApplication.INSTANCE) },
        Param.CLIENT_ID to { clientId },
        Param.CLIENT_TYPE to { "2" },
        Param.OS_VERSION to { Build.VERSION.SDK_INT.toString() },
        Param.MODEL to { Build.MODEL },
        Param.NET_TYPE to { "1" },
        Param.PHONE_IMEI to { MobileInfoUtil.getIMEI(BaseApplication.INSTANCE) },
        Param.TIMESTAMP to { System.currentTimeMillis().toString() }
    )

    private val defaultCommonHeaderInterceptor =
        CommonHeaderInterceptor(
            Header.COOKIE to { "ka=open" },
            Header.PRAGMA to { "no-cache" }
        )
    private val gsonConverterFactory = GsonConverterFactory.create()
    private val sortAndSignInterceptor = SortAndSignInterceptor("tiebaclient!!!")

    val NEW_TIEBA_API: NewTiebaApi by lazy {
        createAPI<NewTiebaApi>("http://c.tieba.baidu.com/",
            defaultCommonHeaderInterceptor,
            CommonHeaderInterceptor(
                Header.USER_AGENT to { "bdtb for Android 8.2.2" },
                Header.CUID to { UIDUtil.getFinalCUID() }
            ),
            defaultCommonParamInterceptor,
            stParamInterceptor,
            CommonParamInterceptor(
                Param.CUID to { UIDUtil.getFinalCUID() },
                Param.FROM to { "baidu_appstore" },
                Param.CLIENT_VERSION to { "8.2.2" }
            ))
    }

    val WEB_TIEBA_API: WebTiebaApi by lazy {
        createAPI<WebTiebaApi>("https://tieba.baidu.com/",
            CommonHeaderInterceptor(
                Header.ACCEPT_LANGUAGE to { Header.ACCEPT_LANGUAGE_VALUE },
                Header.HOST to { "tieba.baidu.com" },
                Header.USER_AGENT to { "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.0 Mobile Safari/537.36 Edg/103.0.1264.2" }
            ),
            AddCookieInterceptor)
    }

    val MINI_TIEBA_API: MiniTiebaApi by lazy {
        createAPI<MiniTiebaApi>("http://c.tieba.baidu.com/",
            defaultCommonHeaderInterceptor,
            CommonHeaderInterceptor(
                Header.USER_AGENT to { "bdtb for Android 7.2.0.0" },
                Header.CUID to { UIDUtil.getFinalCUID() },
                Header.CUID_GALAXY2 to { UIDUtil.getFinalCUID() }
            ),
            defaultCommonParamInterceptor,
            stParamInterceptor,
            CommonParamInterceptor(
                Param.CUID to { UIDUtil.getFinalCUID() },
                Param.CUID_GALAXY2 to { UIDUtil.getFinalCUID() },
                Param.FROM to { "1021636m" },
                Param.CLIENT_VERSION to { "7.2.0.0" },
                Param.SUBAPP_TYPE to { "mini" }
            ))
    }

    val OFFICIAL_TIEBA_API: OfficialTiebaApi by lazy {
        createAPI<OfficialTiebaApi>("http://c.tieba.baidu.com/",
            CommonHeaderInterceptor(
                Header.USER_AGENT to { "bdtb for Android 12.25.1.0" },
                Header.CUID to { CuidUtils.getNewCuid() },
                Header.CUID_GALAXY2 to { CuidUtils.getNewCuid() },
                Header.CUID_GID to { "" },
                Header.CUID_GALAXY3 to { UIDUtil.getAid() },
                Header.CLIENT_TYPE to { "2" },
                Header.CHARSET to { "UTF-8" },
            ),
            defaultCommonParamInterceptor,
            stParamInterceptor,
            CommonParamInterceptor(
                Param.CUID to { CuidUtils.getNewCuid() },
                Param.CUID_GALAXY2 to { CuidUtils.getNewCuid() },
                Param.CUID_GID to { "" },
                Param.FROM to { "tieba" },
                Param.CLIENT_VERSION to { "12.25.1.0" },
                Param.CUID_GALAXY3 to { UIDUtil.getAid() },
                Param.OAID to { OAID(BaseApplication.oaid).toJson() },
            ))
    }

    private inline fun <reified T : Any> createAPI(
        baseUrl: String,
        vararg interceptors: Interceptor
    ) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addCallAdapterFactory(DeferredCallAdapterFactory())
        .addCallAdapterFactory(FlowCallAdapterFactory.create())
        .addConverterFactory(NullOnEmptyConverterFactory())
        .addConverterFactory(gsonConverterFactory)
        .addConverterFactory(ProtoConverterFactory.create())
        .client(OkHttpClient.Builder().apply {
            interceptors.forEach {
                addInterceptor(it)
            }
            addInterceptor(DropInterceptor)
            addInterceptor(sortAndSignInterceptor)
            addInterceptor(FailureResponseInterceptor)
            addInterceptor(ForceLoginInterceptor)
            connectionPool(connectionPool)
        }.build())
        .build()
        .create(T::class.java)
}