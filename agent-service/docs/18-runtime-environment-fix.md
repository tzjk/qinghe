# Phase 4.5 Python 运行环境隔离修复

## 根因

原 `agent-service/.venv` 的解释器路径虽然位于项目内，但其 `pyvenv.cfg` 指向 Anaconda Python 3.12.4，标准库 `ctypes` 也从 Anaconda 加载。导入 `_ctypes` 时发生 DLL access-denied，继而阻断 Click、Uvicorn 和 FastAPI。

父进程的 `CONDA_PREFIX`、`CONDA_DEFAULT_ENV` 和 `CONDA_PROMPT_MODIFIER` 均未设置，但 PATH 含有 Anaconda 及 Library 路径。裸 `uvicorn` 不在父 PATH；旧环境的 `python -m uvicorn` 与其 venv `uvicorn.exe` 均在 Click 导入处失败。

## 实际修复

1. 保留旧 `.venv`，不重装、不删除 Anaconda。
2. 验证独立 CPython 3.12.0 的 `ctypes` 可导入后，在 Agent 内创建 `.venv-clean` 并仅安装本项目声明的服务/测试依赖。
3. `scripts/start_agent.ps1` 和 `scripts/run_tests.ps1` 显式调用 `.venv-clean/Scripts/python.exe`；仅在其子进程清除三个 Conda 变量，并从该子进程 PATH 排除 Anaconda/Library/DLL 路径。
4. 两个 PowerShell 脚本以 UTF-8 BOM 保存，兼容 Windows PowerShell 解析；这不是系统编码设置变更。

`.venv-clean` 的 `ctypes`、Click、FastAPI、Uvicorn 均成功导入，`python -m uvicorn` 与 venv `uvicorn.exe` 均能报告版本。未修改系统 PATH、PowerShell Profile、注册表、Anaconda 安装目录或任何用户已有环境。
