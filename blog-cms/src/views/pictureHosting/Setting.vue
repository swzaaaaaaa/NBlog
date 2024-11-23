<template>
	<div>
		<el-alert title="图床配置及用法请查看：https://github.com/Naccl/PictureHosting" type="warning" show-icon v-if="hintShow"></el-alert>
		<el-card>
			<div slot="header">
				<span>腾讯云存储配置</span>
			</div>
			<el-form :model="txyunConfig" label-width="100px">
				<el-form-item label="secret-id">
					<el-input v-model="txyunConfig.secretId"></el-input>
				</el-form-item>
				<el-form-item label="secret-key">
					<el-input v-model="txyunConfig.secretKey"></el-input>
				</el-form-item>
				<el-form-item label="存储空间名">
					<el-input v-model="txyunConfig.bucketName"></el-input>
				</el-form-item>
				<el-form-item label="地域">
					<el-input v-model="txyunConfig.region"></el-input>
				</el-form-item>
				<el-form-item label="CDN访问域名">
					<el-input v-model="txyunConfig.domain"></el-input>
				</el-form-item>
				<el-button type="primary" size="medium" icon="el-icon-check" :disabled="!isTxyunSave" @click="saveTxyun(true)">保存配置</el-button>
				<el-button type="info" size="medium" icon="el-icon-close" @click="saveTxyun(false)">清除配置</el-button>
			</el-form>
		</el-card>
	</div>
</template>

<script>
import {getUserInfo} from "@/api/github";

export default {
	name: "Setting",
	data() {
		return {
			hintShow: false,
			txyunConfig: {
				secretId: '',
				secretKey: '',
				bucketName: '',
				region: '',
				domain: ''
			},
		}
	},
	computed: {
		isTxyunSave() {
			return this.txyunConfig.secretId && this.txyunConfig.secretKey && this.txyunConfig.bucketName && this.txyunConfig.region && this.txyunConfig.domain
		}
	},
	created() {
		const txyunConfig = localStorage.getItem('txyunConfig')
		if (txyunConfig) {
			this.txyunConfig = JSON.parse(txyunConfig)
		}

		const userJson = window.localStorage.getItem('user') || '{}'
		const user = JSON.parse(userJson)
		if (userJson !== '{}' && user.role !== 'ROLE_admin') {
			//对于访客模式，增加个提示
			this.hintShow = true
		}
	}
	,
	methods: {
		saveTxyun(save) {
			if (save) {
				localStorage.setItem('txyunConfig', JSON.stringify(this.txyunConfig))
				this.msgSuccess('保存成功')
			} else {
				localStorage.removeItem('txyunConfig')
				this.msgSuccess('清除成功')
			}
		}
	}
	,
}
</script>

<style scoped>
.el-alert + .el-row, .el-row + .el-row {
	margin-top: 20px;
}

.el-avatar {
	vertical-align: middle;
	margin-right: 15px;
}

.middle {
	vertical-align: middle;
}

.el-card {
	width: 50%;
}

.el-card + .el-card {
	margin-top: 20px;
}
</style>
