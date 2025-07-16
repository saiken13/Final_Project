// ml-api/train_and_serve.js

process.on('uncaughtException', (err) => {
  console.error('Uncaught Exception:', err.stack);
  process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('Unhandled Rejection at:', promise, 'reason:', reason.stack);
  process.exit(1);
});

const fs = require('fs');
const { parse } = require('csv-parse/sync');
const tf = require('@tensorflow/tfjs-node');
const express = require('express');
const cors = require('cors');

// Load and parse CSV
const path = require('path');
const csvPath = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'homedata.csv');
const csv = fs.readFileSync(csvPath);

// Parse CSV with proper options to handle commas in fields
let records;
try {
  records = parse(csv, { 
    columns: true, 
    skip_empty_lines: true,
    delimiter: ',',
    quote: '"',
    escape: '"',
    relax_column_count: true // Allow inconsistent column counts
  });
} catch (error) {
  console.error('CSV parsing error:', error);
  console.log('Trying alternative parsing...');
  
  // Alternative: Parse without column headers first to inspect structure
  const rawRecords = parse(csv, { 
    skip_empty_lines: true,
    delimiter: ',',
    quote: '"',
    escape: '"'
  });
  
  console.log('First few rows of CSV:');
  rawRecords.slice(0, 3).forEach((row, index) => {
    console.log(`Row ${index}: ${row.length} columns`, row.slice(0, 5));
  });
  
  // If your CSV has headers, manually map them
  if (rawRecords.length > 0) {
    const headers = rawRecords[0];
    console.log('Headers:', headers);
    
    records = rawRecords.slice(1).map(row => {
      const record = {};
      headers.forEach((header, index) => {
        record[header] = row[index] || '';
      });
      return record;
    });
  }
}

console.log('Sample record:', records[0]);
console.log('Available columns:', Object.keys(records[0]));

// Function to safely parse numeric values
function safeParseFloat(value) {
  const parsed = parseFloat(value);
  return isNaN(parsed) ? 0 : parsed;
}

// Extract features from available data
const features = records.map(r => {
  // Use size (square footage) as area
  const area = safeParseFloat(r.size || 0);
  
  // Extract bedrooms and bathrooms from description if possible
  // This is a rough estimation - you should add proper columns to your CSV
  let bedrooms = 0;
  let bathrooms = 0;
  
  if (r.description) {
    const desc = r.description.toLowerCase();
    
    // Try to extract bedroom count
    const bedroomMatch = desc.match(/(\d+)\s+(bedroom|bed)/);
    if (bedroomMatch) {
      bedrooms = parseInt(bedroomMatch[1]);
    } else {
      // Estimate based on size (rough approximation)
      if (area > 2500) bedrooms = 4;
      else if (area > 1800) bedrooms = 3;
      else if (area > 1000) bedrooms = 2;
      else bedrooms = 1;
    }
    
    // Try to extract bathroom count
    const bathroomMatch = desc.match(/(\d+)\s+(bathroom|bath)/);
    if (bathroomMatch) {
      bathrooms = parseInt(bathroomMatch[1]);
    } else {
      // Estimate based on bedrooms
      bathrooms = Math.max(1, Math.floor(bedrooms * 0.75));
    }
  }
  
  return [bedrooms, bathrooms, area];
}).filter(feature => feature[2] > 0); // Filter out records with no area data

const labels = records.map(r => {
  const price = safeParseFloat(r.price || 0);
  return price;
}).filter(price => price > 0); // Filter out invalid prices

// Ensure features and labels have the same length
const minLength = Math.min(features.length, labels.length);
const validFeatures = features.slice(0, minLength);
const validLabels = labels.slice(0, minLength);

console.log(`Using ${validFeatures.length} valid records for training`);

if (validFeatures.length === 0) {
  console.error('No valid records found. Please check your CSV column names and data.');
  process.exit(1);
}

// Normalize features
const featureArray = validFeatures;
const featureTensor = tf.tensor2d(featureArray);
const labelTensor = tf.tensor2d(validLabels, [validLabels.length, 1]);

// Calculate normalization parameters
const featureMean = featureTensor.mean(0);
const featureStd = featureTensor.sub(featureMean).square().mean(0).sqrt();
const normalizedFeatures = featureTensor.sub(featureMean).div(featureStd);

const labelMean = labelTensor.mean();
const labelStd = labelTensor.sub(labelMean).square().mean().sqrt();
const normalizedLabels = labelTensor.sub(labelMean).div(labelStd);

// Create model
const model = tf.sequential();
model.add(tf.layers.dense({ inputShape: [3], units: 64, activation: 'relu' }));
model.add(tf.layers.dense({ units: 32, activation: 'relu' }));
model.add(tf.layers.dense({ units: 16, activation: 'relu' }));
model.add(tf.layers.dense({ units: 1 }));

model.compile({ 
  optimizer: tf.train.adam(0.001), 
  loss: 'meanSquaredError',
  metrics: ['mse']
});

// ... (previous code remains the same until the async block)

(async () => {
  console.log('Training model...');
  const history = await model.fit(normalizedFeatures, normalizedLabels, { 
    epochs: 200,
    validationSplit: 0.2,
    callbacks: {
      onEpochEnd: (epoch, logs) => {
        if (epoch % 50 === 0) {
          console.log(`Epoch ${epoch}: loss = ${logs.loss.toFixed(4)}, val_loss = ${logs.val_loss.toFixed(4)}`);
        }
      }
    }
  });
  
  await model.save('file://./model');
  
  // Save normalization parameters as flat arrays
  const normalizationParams = {
    featureMean: Array.from(await featureMean.data()), // Ensure flat array
    featureStd: Array.from(await featureStd.data()),  // Ensure flat array
    labelMean: Array.from(await labelMean.data()),    // Ensure flat array
    labelStd: Array.from(await labelStd.data())       // Ensure flat array
  };
  
  fs.writeFileSync('./normalization_params.json', JSON.stringify(normalizationParams));
  console.log('Model and normalization parameters saved:', normalizationParams);
  
  startServer();
})();

function startServer() {
  const app = express();
  app.use(cors());
  app.use(express.json());
  
  app.post('/predict', async (req, res) => {
  try {
    const { bedrooms, bathrooms, area } = req.body;
    console.log('Received input:', { bedrooms, bathrooms, area });

    // Load normalization parameters
    const normParams = JSON.parse(fs.readFileSync('./normalization_params.json'));
    console.log('Normalization params loaded:', normParams);

    // Normalize input
    const input = tf.tensor2d([[bedrooms, bathrooms, area]]);
    const featureMeanTensor = tf.tensor1d(normParams.featureMean);
    const featureStdTensor = tf.tensor1d(normParams.featureStd);
    const normalizedInput = input.sub(featureMeanTensor).div(featureStdTensor);
    console.log('Normalized input shape:', normalizedInput.shape);

    // Load model and predict
    const loaded = await tf.loadLayersModel('file://./model/model.json');
    console.log('Model loaded successfully');

    const normalizedOutput = loaded.predict(normalizedInput);
    console.log('Normalized output shape:', normalizedOutput.shape);
    const outputData = normalizedOutput.dataSync();
    console.log('Normalized output data:', outputData);

    const labelMean = normParams.labelMean[0];
    const labelStd = normParams.labelStd[0];
    const denormalizedOutput = tf.mul(normalizedOutput, labelStd).add(labelMean);
    console.log('Denormalized output shape:', denormalizedOutput.shape);

    const predicted = denormalizedOutput.dataSync()[0];
    console.log('Predicted value before rounding:', predicted);

    const predictedPrice = Math.round(Math.max(0, predicted));
    console.log('Sending response:', { predictedPrice }); // Add this
    res.json({ predictedPrice }); // Send without Math.round() wrapper for clarity

    // Dispose tensors to free memory
    input.dispose();
    featureMeanTensor.dispose();
    featureStdTensor.dispose();
    normalizedInput.dispose();
    normalizedOutput.dispose();
    denormalizedOutput.dispose();
    loaded.dispose(); // Dispose the loaded model
  } catch (error) {
    console.error('Prediction error:', error.stack);
    res.status(500).json({ error: 'Prediction failed' });
  }
});
  app.listen(5001, () => console.log('ML API listening on port 5001'));
}