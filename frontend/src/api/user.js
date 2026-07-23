import http from './http'
export const sendCode=(phone)=>http.post('/user/code',{phone})
export const register=(data)=>http.post('/auth/register',data)
export const login=(phone,code)=>http.post('/user/login',{phone,code})
export const passwordLogin=(phone,password)=>http.post('/user/login/password',{phone,password})
export const completeInitialProfile=(data)=>http.post('/user/profile/complete',data)
export const getMe=()=>http.get('/user/me')
export const updateProfile=(data)=>http.put('/user/profile',data)
export const uploadAvatar=(file)=>{const form=new FormData();form.append('file',file,'avatar.webp');return http.post('/user/avatar',form)}
export const logout=()=>http.post('/user/logout')
