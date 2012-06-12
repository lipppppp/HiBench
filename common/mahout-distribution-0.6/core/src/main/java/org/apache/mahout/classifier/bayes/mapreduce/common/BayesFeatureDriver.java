/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.classifier.bayes.mapreduce.common;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.mahout.classifier.bayes.BayesParameters;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.common.StringTuple;

import java.io.IOException;

/**
 * Create and run the Bayes Feature Reader Step.
 */
public class BayesFeatureDriver implements BayesJob {

  @Override
  public void runJob(Path input, Path output, BayesParameters params) throws IOException {
    Configurable client = new JobClient();
    JobConf conf = new JobConf(BayesFeatureDriver.class);
    conf.setJobName("Bayes Feature Driver running over input: " + input);
    conf.setOutputKeyClass(StringTuple.class);
    conf.setOutputValueClass(DoubleWritable.class);
    conf.setPartitionerClass(FeaturePartitioner.class);
    conf.setOutputKeyComparatorClass(FeatureLabelComparator.class);
    FileInputFormat.setInputPaths(conf, input);
    FileOutputFormat.setOutputPath(conf, output);

    conf.setMapperClass(BayesFeatureMapper.class);

    conf.setInputFormat(SequenceFileInputFormat.class);
    conf.setCombinerClass(BayesFeatureCombiner.class);
    conf.setReducerClass(BayesFeatureReducer.class);
    conf.setOutputFormat(BayesFeatureOutputFormat.class);
    conf.set("io.serializations",
            "org.apache.hadoop.io.serializer.JavaSerialization,org.apache.hadoop.io.serializer.WritableSerialization");
    // this conf parameter needs to be set enable serialisation of conf values

    HadoopUtil.delete(conf, output);
    conf.set("bayes.parameters", params.toString());
    if (params.get("compress").equals("true")) {
  	  conf.setBoolean("mapred.output.compress", true);
  	  conf.set("mapred.output.compression.type", "BLOCK");
  	  conf.set("mapred.output.compression.codec", params.get("codec"));
  	} else {
  	  conf.setBoolean("mapred.output.compress", false);
  	}

    client.setConf(conf);
    JobClient.runJob(conf);

  }
}
