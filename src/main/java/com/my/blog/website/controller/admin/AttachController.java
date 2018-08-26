package com.my.blog.website.controller.admin;

import com.github.pagehelper.PageInfo;
import com.my.blog.website.constant.WebConst;
import com.my.blog.website.controller.BaseController;
import com.my.blog.website.dto.LogActions;
import com.my.blog.website.dto.Types;
import com.my.blog.website.model.Bo.RestResponseBo;
import com.my.blog.website.model.Vo.AttachVo;
import com.my.blog.website.model.Vo.UserVo;
import com.my.blog.website.service.IAttachService;
import com.my.blog.website.service.ILogService;
import com.my.blog.website.utils.Commons;
import com.my.blog.website.utils.TaleUtils;
import com.my.blog.website.utils.qiniu.QiniuCloudUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 附件管理
 * <p>
 * Created by 13 on 2017/2/21.
 */
@Controller
@RequestMapping("admin/attach")
public class AttachController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachController.class);

    public static final String CLASSPATH = TaleUtils.getUploadFilePath();

    @Resource
    private IAttachService attachService;

    @Resource
    private ILogService logService;

    /**
     * 附件页面
     *
     * @param request
     * @param page
     * @param limit
     * @return
     */
    @GetMapping(value = "")
    public String index(HttpServletRequest request, @RequestParam(value = "page", defaultValue = "1") int page,
                        @RequestParam(value = "limit", defaultValue = "12") int limit) {
        PageInfo<AttachVo> attachPaginator = attachService.getAttachs(page, limit);
        request.setAttribute("attachs", attachPaginator);
        request.setAttribute(Types.ATTACH_URL.getType(), Commons.site_option(Types.ATTACH_URL.getType(), Commons.site_url()));
        request.setAttribute("max_file_size", WebConst.MAX_FILE_SIZE / 1024);
        return "admin/attach";
    }

    /**
     * 上传文件接口 保存本地服务器
     *
     * @param request
     * @return
     */
    @PostMapping(value = "upload1")
    @ResponseBody
    public RestResponseBo upload(HttpServletRequest request, @RequestParam("file") MultipartFile[] multipartFiles) throws IOException {
        UserVo users = this.user(request);
        Integer uid = users.getUid();
        List<String> errorFiles = new ArrayList<>();
        try {
            for (MultipartFile multipartFile : multipartFiles) {
                String fname = multipartFile.getOriginalFilename();
                if (multipartFile.getSize() <= WebConst.MAX_FILE_SIZE) {
                    String fkey = TaleUtils.getFileKey(fname);
                    String ftype = TaleUtils.isImage(multipartFile.getInputStream()) ? Types.IMAGE.getType() : Types.FILE.getType();
                    File file = new File(CLASSPATH + fkey);
                    try {
                        FileCopyUtils.copy(multipartFile.getInputStream(), new FileOutputStream(file));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    attachService.save(fname, fkey, ftype, uid);
                } else {
                    errorFiles.add(fname);
                }
            }
        } catch (Exception e) {
            return RestResponseBo.fail();
        }
        return RestResponseBo.ok(errorFiles);
    }

    /**
     * 上传文件接口  保存七牛服务器
     *
     * @param request
     * @return
     */
    @PostMapping(value = "upload")
    @ResponseBody
    public RestResponseBo upload2QINIU(HttpServletRequest request, @RequestParam("file") MultipartFile[] multipartFiles) throws Exception {

        UserVo users = this.user(request);
        Integer uid = users.getUid();
        List<String> errorFiles = new ArrayList<>();

        QiniuCloudUtil qiniuUtil = new QiniuCloudUtil();
        if (multipartFiles != null && multipartFiles.length > 0) {

            try {
                for (MultipartFile image : multipartFiles) {
                        String fname = image.getOriginalFilename();
                        if (image.getSize() <= WebConst.MAX_FILE_SIZE) {
                            byte[] bytes = image.getBytes();
                            String imageName = UUID.randomUUID().toString()+fname.substring(fname.lastIndexOf("."));
                            String ftype = TaleUtils.isImage(image.getInputStream()) ? Types.IMAGE.getType() : Types.FILE.getType();
                            try {
                                //使用base64方式上传到七牛云
                                String url = qiniuUtil.put64image(bytes, imageName);
                                url="http://"+url;
                                attachService.save(fname, url, ftype, uid);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            errorFiles.add(fname);
                        }
                }
            } catch (Exception e) {
                return RestResponseBo.fail();
            }

        }
        return RestResponseBo.ok(errorFiles);
    }


    @RequestMapping(value = "delete1")
    @ResponseBody
    public RestResponseBo delete(@RequestParam Integer id, HttpServletRequest request) {
        try {
            AttachVo attach = attachService.selectById(id);
            if (null == attach) {
                return RestResponseBo.fail("不存在该附件");
            }
            attachService.deleteById(id);
            new File(CLASSPATH + attach.getFkey()).delete();
            logService.insertLog(LogActions.DEL_ARTICLE.getAction(), attach.getFkey(), request.getRemoteAddr(), this.getUid(request));
        } catch (Exception e) {
            String msg = "附件删除失败";
            LOGGER.error(msg, e);
            return RestResponseBo.fail(msg);
        }
        return RestResponseBo.ok();
    }

    @RequestMapping(value = "delete")
    @ResponseBody
    public RestResponseBo deleteFromQiniu(@RequestParam Integer id, HttpServletRequest request) {
        try {

            AttachVo attach = attachService.selectById(id);
            if (null == attach) {
                return RestResponseBo.fail("不存在该附件");
            }
            //先从七牛服务器上删除数据
            QiniuCloudUtil qiniuCloudUtil = new QiniuCloudUtil();
            String fkey = attach.getFkey();
            String key = fkey.substring(fkey.lastIndexOf("/")+1);
            qiniuCloudUtil.delete(key);

            //从数据库删除记录
            attachService.deleteById(id);

            //记录日志
            logService.insertLog(LogActions.DEL_ARTICLE.getAction(), attach.getFkey(), request.getRemoteAddr(), this.getUid(request));
        } catch (Exception e) {
            String msg = "附件删除失败";
            LOGGER.error(msg, e);
            return RestResponseBo.fail(msg);
        }
        return RestResponseBo.ok();
    }


}