# ml-model/train_model.py

import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LinearRegression
import joblib

# Sample dataset
data = {
    'bedrooms': [2, 3, 3, 4, 4, 5],
    'bathrooms': [1, 2, 2, 3, 3, 4],
    'area': [1000, 1500, 1600, 2000, 2100, 2500],
    'price': [200000, 300000, 320000, 400000, 420000, 500000]
}

df = pd.DataFrame(data)

# Features and target
X = df[['bedrooms', 'bathrooms', 'area']]
y = df['price']

# Train model
model = LinearRegression()
model.fit(X, y)

# Save model
joblib.dump(model, 'price_model.pkl')
print("✅ Model trained and saved as price_model.pkl")
