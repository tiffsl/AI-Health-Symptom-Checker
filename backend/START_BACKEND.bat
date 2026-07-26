@echo off
echo ========================================
echo  CP2 Symptom Checker - Backend Server
echo ========================================
echo.
echo Installing required packages...
pip install flask scikit-learn numpy --quiet
echo.
echo Starting backend on port 5050...
echo Keep this window open while using the app!
echo.
python app.py
pause
