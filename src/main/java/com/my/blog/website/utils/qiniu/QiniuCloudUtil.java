package com.my.blog.website.utils.qiniu;

import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;
import com.qiniu.util.Base64;
import com.qiniu.util.StringMap;
import com.qiniu.util.UrlSafeBase64;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class QiniuCloudUtil {

    // 设置需要操作的账号的AK和SK
    private static final String ACCESS_KEY = "m72-OKkjigCfLcdkyD-wUagPY6ywZHzBOvMskFIK";
    private static final String SECRET_KEY = "6pkU7hEHJpHGbq51Gphewg1PQsPuFzUBJ3wFX5k1";

    // 要上传的空间
    private static final String bucketname = "iblog";

    // 密钥
    private static final Auth auth = Auth.create(ACCESS_KEY, SECRET_KEY);

    private static final String DOMAIN = "pe0l5o3o1.bkt.clouddn.com";

    private static final String style = "自定义的图片样式";

    public String getUpToken() {
        return auth.uploadToken(bucketname, null, 3600, new StringMap().put("insertOnly", 1));
    }
    // 普通上传
    public String upload(String filePath, String fileName) throws IOException {
        // 创建上传对象
        Configuration config = new Configuration(Zone.autoZone());
        UploadManager uploadManager = new UploadManager(config);
        try {
            // 调用put方法上传
            String token = auth.uploadToken(bucketname);
            if(StringUtils.isEmpty(token)) {
                System.out.println("未获取到token，请重试！");
                return null;
            }
            Response res = uploadManager.put(filePath, fileName, token);
            // 打印返回的信息
            System.out.println(res.bodyString());
            if (res.isOK()) {
                Ret ret = res.jsonToObject(Ret.class);
                //如果不需要对图片进行样式处理，则使用以下方式即可
                //return DOMAIN + ret.key;
                return DOMAIN + ret.key + "?" + style;
            }
        } catch (QiniuException e) {
            Response r = e.response;
            // 请求失败时打印的异常的信息
            System.out.println(r.toString());
            try {
                // 响应的文本信息
                System.out.println(r.bodyString());
            } catch (QiniuException e1) {
                // ignore
            }
        }
        return null;
    }


    //base64方式上传
    public String put64image(byte[] base64, String key) throws Exception{
        String file64 = Base64.encodeToString(base64, 0);
        Integer l = base64.length;
        String url = "http://up-z2.qiniup.com/putb64/" + l + "/key/"+ UrlSafeBase64.encodeToString(key);
        //非华东空间需要根据注意事项 1 修改上传域名
        RequestBody rb = RequestBody.create(null, file64);
        Request request = new Request.Builder().
                url(url).
                addHeader("Content-Type", "application/octet-stream")
                .addHeader("Authorization", "UpToken " + getUpToken())
                .post(rb).build();
        //System.out.println(request.headers());
        OkHttpClient client = new OkHttpClient();
        okhttp3.Response response = client.newCall(request).execute();
        System.out.println(response);
        //如果不需要添加图片样式，使用以下方式
        return DOMAIN + "/"+key;
        //return DOMAIN + key + "?" + style;
    }


    // 普通删除
    public void delete(String key){
        Configuration config = new Configuration(Zone.autoZone());
        BucketManager bucketMgr = new BucketManager(auth, config);
        //指定需要删除的文件，和文件所在的存储空间
        try {
            bucketMgr.delete(bucketname, key);//当前为7.2.1；  7.2.2后才能传多个key ，即：第二个参数为数组 (String... deleteTargets)
        } catch (QiniuException e) {
            e.printStackTrace();
        }
    }

    /*
    获取空间文件列表
     */
    public BucketManager.FileListIterator getFileList(){
        Configuration config = new Configuration(Zone.autoZone());
        BucketManager bucketMgr = new BucketManager(auth, config);

        //文件名前缀
        String prefix = "";
        //每次迭代的长度限制，最大1000，推荐值 1000
        int limit = 1000;
        //指定目录分隔符，列出所有公共前缀（模拟列出目录效果）。缺省值为空字符串
        String delimiter = "";
        //列举空间文件列表
        BucketManager.FileListIterator fileListIterator = bucketMgr.createFileListIterator(bucketname, prefix, limit, delimiter);
        return fileListIterator;
    }

    class Ret {
        public long fsize;
        public String key;
        public String hash;
        public int width;
        public int height;
    }

 public static  void main(String[] args) throws Exception {
     //构造一个带指定Zone对象的配置类
     Configuration cfg = new Configuration(Zone.zone0());
//...其他参数参考类注释


     Auth auth = Auth.create(QiniuCloudUtil.ACCESS_KEY, QiniuCloudUtil.SECRET_KEY);
     BucketManager bucketManager = new BucketManager(auth, cfg);

//文件名前缀
     String prefix = "";
//每次迭代的长度限制，最大1000，推荐值 1000
     int limit = 1000;
//指定目录分隔符，列出所有公共前缀（模拟列出目录效果）。缺省值为空字符串
     String delimiter = "";

//列举空间文件列表
     BucketManager.FileListIterator fileListIterator = bucketManager.createFileListIterator(bucketname, prefix, limit, delimiter);
     while (fileListIterator.hasNext()) {
         //处理获取的file list结果
         FileInfo[] items = fileListIterator.next();
         System.out.println(items.length);
         for (FileInfo item : items) {
             System.out.println(item.key);
             System.out.println(item.hash);
             System.out.println(item.fsize);
             System.out.println(item.mimeType);
             System.out.println(item.putTime);
             System.out.println(item.endUser);
         }
     }

 }

}

