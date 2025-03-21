<template>
	<div>
		<el-row>
			<el-select v-model="txyunConfig.bucketName" disabled style="min-width: 200px"></el-select>
			<el-cascader v-model="activePath" placeholder="请选择目录" :options="pathArr" :props="pathProps" style="min-width: 450px"></el-cascader>
			<el-button type="primary" size="medium" icon="el-icon-search" @click="search">查询</el-button>
			<el-button class="right-item" type="primary" size="medium" icon="el-icon-upload" @click="isDrawerShow=!isDrawerShow">上传</el-button>
		</el-row>
		<el-alert title="只显示<img>标签支持的 apng,avif,bmp,gif,ico,cur,jpg,jpeg,jfif,pjpeg,pjp,png,svg,tif,tiff,webp 格式的图片，见 https://developer.mozilla.org/zh-CN/docs/Web/HTML/Element/img" type="warning" show-icon close-text="不再提示" v-if="hintShow1" @close="noDisplay(1)"></el-alert>
		<el-alert title="最多显示100个文件" type="warning" show-icon close-text="不再提示" v-if="hintShow2" @close="noDisplay(2)"></el-alert>
		<el-row v-viewer>
			<div class="image-container" v-for="(file,index) in fileList" :key="index">
				<el-image :src="file.cdn_url" fit="scale-down"></el-image>
				<div class="image-content">
					<div class="info">
						<span>{{ file.name }}</span>
					</div>
					<div class="icons">
						<el-tooltip class="item" effect="dark" content="复制CDN链接" placement="bottom">
							<i class="icon el-icon-link" @click="copy(1,file)"></i>
						</el-tooltip>
						<el-tooltip class="item" effect="dark" content="复制MD格式" placement="bottom">
							<SvgIcon icon-class="markdown" class-name="icon" @click="copy(2,file)"></SvgIcon>
						</el-tooltip>
						<i class="icon el-icon-delete" @click="delFile(file)"></i>
					</div>
				</div>
			</div>
		</el-row>

		<el-drawer title="上传文件" :visible.sync="isDrawerShow" direction="rtl" size="40%" :wrapperClosable="false" :close-on-press-escape="false">
			<el-row>
				<el-radio v-model="nameType" label="1">使用源文件名</el-radio>
				<el-radio v-model="nameType" label="2">使用UUID文件名</el-radio>
				<el-button size="small" type="primary" icon="el-icon-upload" v-throttle="[submitUpload,`click`,3000]">确定上传</el-button>
			</el-row>
			<el-row>
				当前目录：{{ realPath }}
			</el-row>
			<el-row>
				<el-switch v-model="isCustomPath" active-text="自定义目录"></el-switch>
				<el-input placeholder="例：oldFolder/newFolder/" v-model="customPath" :disabled="!isCustomPath" size="medium" style="margin-top: 10px"></el-input>
			</el-row>
			<el-upload ref="uploadRef" action="" :http-request="upload" drag multiple :file-list="uploadList" list-type="picture" :auto-upload="false">
				<i class="el-icon-upload"></i>
				<div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
			</el-upload>
		</el-drawer>
	</div>
</template>

<script>
import SvgIcon from "@/components/SvgIcon";
import {isImgExt} from "@/util/validate";
import {randomUUID} from "@/util/uuid";
import {copy} from "@/util/copy";
import COS from 'cos-js-sdk-v5';

export default {
	name: "TxyunManage",
	components: {SvgIcon},
	data() {
		return {
			cos: {},
			txyunConfig: {
				secretId: '',
				secretKey: '',
				bucketName: '',
				region: '',
				domain: ''
			},
			pathArr: [{value: '', label: '根目录'}],
			activePath: [''],//默认选中目录--图片目录
			pathProps: {
				lazy: true,
				checkStrictly: true,
				lazyLoad: async (node, resolve) => {
					let path = node.path.join('/')
					path = path.startsWith('/') ? path.slice(1) : path
					if (path != '') {
						path = path.endsWith('/') ? path : `${path}/`
					}
					let nodes = []
					await this.getReposContents(nodes, path)
					resolve(nodes)
				}
			},
			hintShow1: true,
			hintShow2: true,
			fileList: [],
			isDrawerShow: false,
			nameType: '1',
			uploadList: [],
			isCustomPath: false,
			customPath: '',
		}
	},
	computed: {
		realPath() {
			if (this.isCustomPath) {
				return `${this.customPath}`
			}
			let path = this.activePath.join('/')
			path = path.startsWith('/') ? path.slice(1) : path
			if (path != '') {
				return `${path}/`
			} else {
				return `${path}`
			}
		}
	},
	created() {
		this.hintShow1 = localStorage.getItem('txyunHintShow1') ? false : true
		this.hintShow2 = localStorage.getItem('txyunHintShow2') ? false : true

		const txyunConfig = localStorage.getItem('txyunConfig')
		if (txyunConfig) {
			this.txyunConfig = JSON.parse(txyunConfig)
			this.txyunConfig.domain = this.txyunConfig.domain.endsWith('/') ? this.txyunConfig.domain : `${this.txyunConfig.domain}/`
			//初始化cos
			this.cos = new COS({
				SecretId: this.txyunConfig.secretId,
				SecretKey: this.txyunConfig.secretKey,
			})
		} else {
			this.msgError('请先配置腾讯云')
			this.$router.push('/pictureHosting/setting')
		}
	},
	methods: {
		//换成懒加载
		async getReposContents(arr, path) {
			const {txyunConfig} = this
			await this.cos.getBucket({
				Bucket: txyunConfig.bucketName, /* 必须 */
				Region: txyunConfig.region,     /* 存储桶所在地域，必须字段 */
				Prefix: path,           /* 非必须 */
				Delimiter: '/'
			}).then(data => {
				data.CommonPrefixes.forEach((item) => {
					item.name = item.Prefix.replace(path, '').slice(0, -1)
					//让所有节点都是非叶子节点
					arr.push({value: item.name, label: item.name, leaf: false})
				});
			})
		},
		// 获取存储空间下指定目录的文件列表
		search() {
			const {txyunConfig} = this
			const fileList = []
			let path = this.activePath.join('/')
			path = path.startsWith('/') ? path.slice(1) : path
			if (path != '') {
				path = path.endsWith('/') ? path : `${path}/`
			}
			this.cos.getBucket({
				Bucket: txyunConfig.bucketName, /* 必须 */ 
				Region: txyunConfig.region,     /* 存储桶所在地域，必须字段 */
				Prefix: path,           /* 非必须 */
				Delimiter: '/'
			}).then(data => {
				data.Contents.filter((item) => !item.Key.endsWith('/') && isImgExt(item.Key)).forEach(item => {
					item.path = item.Key
					item.cdn_url = `${txyunConfig.domain}${item.path}`
					item.name = item.Key.replace(path, '')
					fileList.push(item)
				})
			})
			this.fileList = fileList
		},
		noDisplay(id) {
			localStorage.setItem(`txyunHintShow${id}`, '1')
		},
		copy(type, file) {
			// type 1 cdn link  2 Markdown
			let copyCont = ''
			if (type == 1) {
				copyCont = file.cdn_url
			} else if (type == 2) {
				copyCont = `![${file.name}](${file.cdn_url})`
			}
			copy(copyCont)
			this.msgSuccess('复制成功')
		},
		// 删除文件
		delFile(file) {
			const {txyunConfig} = this
			this.$confirm("此操作将永久删除该文件, 是否删除?", "提示", {
				confirmButtonText: '确定',
				cancelButtonText: '取消',
				type: 'warning',
			}).then(() => {
				this.cos.deleteObject({
					Bucket: txyunConfig.bucketName, /* 填写自己的bucket，必须字段 */
					Region: txyunConfig.region,     /* 存储桶所在地域，必须字段 */
					Key: file.path,              /* 存储在桶里的对象键（例如1.jpg，a/b/test.txt），必须字段 */
				}).then(() => {
					this.msgSuccess('删除成功')
					this.search()
				}).catch(() => {
					this.msgError('删除失败')
				})
			}).catch(() => {
				this.$message({
					type: 'info',
					message: '已取消删除',
				})
			})
		},
		submitUpload() {
			//https://github.com/ElemeFE/element/issues/12080
			this.uploadList = this.$refs.uploadRef.uploadFiles
			if (this.uploadList.length) {
				//触发 el-upload 中 http-request 绑定的函数
				this.$refs.uploadRef.submit()
			} else {
				this.msgError('请先选择文件')
			}
		},
		upload(data) {
			let fileName = data.file.name
			if (this.nameType === '2') {
				fileName = randomUUID() + fileName.substr(fileName.lastIndexOf("."))
			}
			const {txyunConfig} = this
			let path = this.realPath;
			path = path.startsWith('/') ? path.slice(1) : path
			path = path.endsWith('/') ? path : `${path}/`
			this.cos.uploadFile({
				Bucket: txyunConfig.bucketName, /* 必须 */
				Region: txyunConfig.region,     /* 存储桶所在地域，必须字段 */
				Key: `${path}${fileName}`,              /* 存储在桶里的对象键（例如:1.jpg，a/b/test.txt，图片.jpg）支持中文，必须字段 */
				Body: data.file, // 上传文件对象
				SliceSize: 1024 * 1024 * 5,     /* 触发分块上传的阈值，超过5MB使用分块上传，小于5MB使用简单上传。可自行设置，非必须 */
				onProgress: function (progressData) {
					console.log(JSON.stringify(progressData));
				}
			}).then(() => {
				this.msgSuccess('上传成功')
				data.onSuccess()
			})
		},
	},
}
</script>

<style>
.el-drawer__body {
	margin: 0 20px;
	overflow: auto;
}

.el-drawer__body .el-upload {
	margin-top: 20px;
}

.el-drawer__body .el-upload, .el-drawer__body .el-upload-dragger {
	width: 100%;
}
</style>

<style scoped>
.el-row + .el-row, .el-row + .el-alert, .el-alert + .el-row {
	margin-top: 20px;
}

.el-alert + .el-alert {
	margin-top: 10px;
}

.el-select + .el-cascader, .el-cascader + .el-button, .el-button + .el-switch {
	margin-left: 10px;
}

.el-switch + .el-button {
	margin-left: 30px;
}

.image-container {
	position: relative;
	width: 200px;
	height: 200px;
	overflow: hidden;
	display: inline-block;
	margin: 0 2px;
}

.el-image {
	width: 100%;
	height: 100%;
	z-index: 1;
}

.image-content {
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
}

.image-content .info {
	background: linear-gradient(0deg, transparent, rgba(0, 0, 0, .6));
	position: absolute;
	left: 0;
	right: 0;
	top: 0;
	padding: 6px;
	font-size: 12px;
	color: #fff;
	opacity: 0;
	transition: opacity 0.5s;
	z-index: 2;
}

.image-content .info span {
	word-wrap: break-word;
}

.image-container:hover .image-content .info {
	opacity: 1;
	transition-duration: 0.25s;
}

.icons {
	position: absolute;
	right: 8px;
	bottom: 8px;
	z-index: 2;
}

.icon {
	text-decoration: none;
	font-size: 22px;
	margin-left: 20px;
	transform: translateY(80px);
	cursor: pointer;
}

.image-container:hover .icon {
	transform: translateY(0px);
}

.icon:nth-child(1) {
	transition: transform 0.25s 0.05s, color 0.3s;
}

.icon:nth-child(2) {
	transition: transform 0.25s 0.1s, color 0.3s;
}

.icon:nth-child(3) {
	transition: transform 0.25s 0.15s, color 0.3s;
}

.icon:nth-child(1):hover {
	color: #409EFF;
}

.icon:nth-child(2):hover {
	color: #409EFF;
}

.icon:nth-child(3):hover {
	color: #F56C6C;
}

.image-content::before {
	content: '';
	position: absolute;
	left: 0;
	bottom: 0;
	background: rgba(255, 255, 255, 0.7);
	width: 100%;
	height: 0;
	clip-path: polygon(0 100%, 100% 0, 100% 100%);
	transition: 0.3s;
	z-index: 2;
}

.image-container:hover .image-content::before {
	height: 80px;
}
</style>
